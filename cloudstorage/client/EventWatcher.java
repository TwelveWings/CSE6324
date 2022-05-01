package cloudstorage.client;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.IOException;
import java.net.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class EventWatcher extends Thread
{
    public TCPManager tcpm;
    public UDPManager udpm;
    public InetAddress address;
    public String directory;

    public EventWatcher(TCPManager tcp, UDPManager udp, InetAddress addr, String d)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        directory = d;
    }

    @SuppressWarnings("unchecked")
    static<T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>)event;
    }

    public void run()
    {
        Set<String> fileEvents = new HashSet<String>();

        // Concurrent hashmap to store all Reader threads
        ConcurrentHashMap<String, FileReader> readers = new ConcurrentHashMap<String, FileReader>();

        try
        {
            // Watcher service to be used to watch changes in the specified directory.
            WatchService watcher = FileSystems.getDefault().newWatchService();

            // Bytes of file being read
            byte[] data = null;

            // Local directory converted to a Path.
            Path clientDirectory = Paths.get(directory);

            // Watch key will keep track of ENTRY_CREATE, ENTRY_DELETE, and ENTRY MODIFY events.
            WatchKey key = null;

            while(true)
            {
                try
                {
                    key = clientDirectory.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                }

                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }

                for(WatchEvent<?> event : key.pollEvents())
                {
                    WatchEvent.Kind<?> kind = event.kind();

                    if(kind == OVERFLOW)
                    {
                        continue;
                    }

                    WatchEvent<Path> ev = cast(event);
                    Path fileName = ev.context();

                    try
                    {
                        Path child = clientDirectory.resolve(fileName);

                        Thread.sleep(1000);

                        // If the event is a create or modify event begin "upload" synchronization
                        if((kind == ENTRY_CREATE || kind == ENTRY_MODIFY) && !fileEvents.contains(fileName.toString()))
                        {
                            // When a file is added to a directory, it creates a ENTRY_CREATE event and an ENTRY_MODIFY event in rapid succession because
                            // the file is created and then its timestamp is modified. As such, if a thread is still processing a request prevent the system
                            // from creating a new thread. Otherwise, remove the thread from the hashmap and allow it to create a new thread.
                            if(readers.containsKey(fileName.toString()) && readers.get(fileName.toString()).getComplete())
                            {
                                readers.remove(fileName.toString());
                            }

                            if(readers.containsKey(fileName.toString()))
                            {
                                continue;
                            }

                            readers.put(fileName.toString(), new FileReader(fileName.toString(), SystemAction.Upload, tcpm, udpm, 2023, address, directory));
                            readers.get(fileName.toString()).start();
                        }

                        else if(kind == ENTRY_DELETE)
                        {
                            readers.put(fileName.toString(), new FileReader(fileName.toString(), SystemAction.Delete, tcpm, udpm, 2023, address, directory));
                            readers.get(fileName.toString()).start();                            
                        }
                    }

                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                // Reset the key. This is essential according to Oracle API.
                boolean valid = key.reset();

                if(!valid)
                {
                    break;
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
