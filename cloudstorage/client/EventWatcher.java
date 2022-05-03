package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;

import java.io.File;
import java.io.IOException;
import java.net.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class EventWatcher extends Thread
{
    public TCPManager tcpm;
    public UDPManager udpm;
    public InetAddress address;
    public String directory;
    public Synchronizer sync;
    public BoundedBuffer boundedBuffer;
    public HashMap<String, FileData> originalFilesInDirectory;

    public EventWatcher(TCPManager tcp, UDPManager udp, InetAddress addr, String d, BoundedBuffer bb, Synchronizer s, HashMap<String, FileData> ofid)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        directory = d;
        boundedBuffer = bb;
        sync = s;
        originalFilesInDirectory = ofid;
    }

    @SuppressWarnings("unchecked")
    static<T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>)event;
    }

    public void run()
    {
        Set<String> fileEvents = new HashSet<String>();
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

            FileData lastModified = null;

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

                    System.out.println("EVENT THAT WAS FIRED");

                    System.out.println(kind);

                    if(originalFilesInDirectory.containsKey(fileName.toString()) && originalFilesInDirectory.get(fileName.toString()).getSystemCreated())
                    {
                        System.out.println("EVENT IN IF STATEMENT");
                        System.out.println(kind);
//                        originalFilesInDirectory.get(fileName.toString()).setSystemCreated(false);
                        continue;
                    }

                    else
                    {
                        if(lastModified != null)
                        {
                            lastModified.setSystemCreated(false);
                        }

                        lastModified = originalFilesInDirectory.get(fileName.toString());
                    }


                    try
                    {
                        Path child = clientDirectory.resolve(fileName);

                        Thread.sleep(1000);

                        // If the event is a create or modify event begin "upload" synchronization
                        if((kind == ENTRY_MODIFY))
                        {   
                            FileReader fr = new FileReader(fileName.toString(), SystemAction.Upload, tcpm, udpm, 2023, address, directory, boundedBuffer, sync, originalFilesInDirectory.get(fileName.toString()));

                            //Update Hashmap for any modified file or created file before running threads
                            
                            byte[] sendData = Files.readAllBytes(fileName);

                            FileData fileData = new FileData(sendData, fileName.toString(), sendData.length);
                            
                            fileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);
                            
                            if(originalFilesInDirectory.containsKey(fileName.toString()))
                            {
                                originalFilesInDirectory.remove(fileName.toString());
                            }
                            
                            originalFilesInDirectory.put(fileName.toString(), fileData);

                            // run thread
                            fr.start();
                            fr.join();
                        }

                        else if(kind == ENTRY_DELETE)
                        {
                            originalFilesInDirectory.remove(fileName.toString());

                            FileReader fr = new FileReader(fileName.toString(), SystemAction.Delete, tcpm, udpm, 2023, address, directory, boundedBuffer, sync);
                            fr.start();
                            fr.join();
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
