package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.io.IOException;
import java.net.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class EventWatcher extends Thread
{
    public ClientUI ui;
    public TCPManager tcpm;
    public UDPManager udpm;
    public InetAddress address;
    public String directory;
    public Synchronizer sync;
    public Synchronizer downloadSync;
    public Synchronizer uploadSync;
    public BoundedBuffer boundedBuffer;

    public EventWatcher(TCPManager tcp, UDPManager udp, InetAddress addr, String d, BoundedBuffer bb, 
        Synchronizer s, Synchronizer ds, Synchronizer us, ClientUI u)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        directory = d;
        boundedBuffer = bb;
        sync = s;
        downloadSync = ds;
        uploadSync = us;
        ui = u;
    }

    /*
     * \brief cast
     * 
     * Casts WatchEvent<Path> to WatchEvent<T>. The standard cast causes the Java compiler to throw a
     * warning. @SuppressWarnings bypasses this.
     * 
     * \param event is the WatchEvent<Path> that is being cast to WatchEvent<T>
     * 
     * Returns the object that has been cast as WatchEvent<T>
     */
    @SuppressWarnings("unchecked")
    static<T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>)event;
    }

    public void run()
    {
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

            FileController fc = new FileController(tcpm, udpm, sync, uploadSync, boundedBuffer, address, 2023, ui);

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

                    System.out.println("FILE PROCESSED:");
                    System.out.println(fileName.toString());
                    System.out.println(kind);

                    EventProcessor ep = new EventProcessor(fileName.toString(), downloadSync, uploadSync,
                        directory, clientDirectory, kind, fc);      
                        
                    ep.start();
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
