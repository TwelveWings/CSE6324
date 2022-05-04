package cloudstorage.client;

import cloudstorage.enums.*;
import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.net.*;
import java.nio.file.*;

public class EventProcessor extends Thread
{
    public BoundedBuffer boundedBuffer;
    public InetAddress address;
    public Path synchronizedDirectory;
    public String directory;
    public String fileName;
    public Synchronizer downloadSync;
    public Synchronizer sync;
    public Synchronizer uploadSync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public WatchEvent.Kind<?> kind;

    public EventProcessor(TCPManager tcp, UDPManager udp, InetAddress a, String fn, Synchronizer ds, 
        Synchronizer s, Synchronizer us, String d, Path sd, WatchEvent.Kind<?> k, BoundedBuffer bb)
    {
        tcpm = tcp;
        udpm = udp;
        address = a;
        fileName = fn;
        downloadSync = ds;
        uploadSync = us;
        sync = s;
        directory = d;
        synchronizedDirectory = sd;
        kind = k;
        boundedBuffer = bb;
    }   
    
    public void run()
    {
        if((downloadSync.blockedFiles.containsKey(fileName) && downloadSync.blockedFiles.get(fileName)) ||
           (uploadSync.blockedFiles.containsKey(fileName) && uploadSync.blockedFiles.get(fileName)))
        {
            System.out.printf("DOWNLOAD BLOCKED: %b\n", downloadSync.blockedFiles.get(fileName));
            System.out.printf("UPLOAD BLOCKED: %b\n", uploadSync.blockedFiles.get(fileName));
            return;
        }

        if(uploadSync.blockedFiles.containsKey(fileName) && !uploadSync.blockedFiles.get(fileName))
        {
            uploadSync.blockedFiles.replace(fileName, true);
        }

        else if(!uploadSync.blockedFiles.containsKey(fileName))
        {
            uploadSync.blockedFiles.put(fileName, true);
        }
        
        try
        {
            Path child = synchronizedDirectory.resolve(fileName);

            Thread.sleep(1000);

            // If the event is a create or modify event begin "upload" synchronization
            if(kind == ENTRY_CREATE || kind == ENTRY_MODIFY)
            {
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Upload, tcpm, udpm, 2023,
                    address, directory, boundedBuffer, sync, uploadSync);

                fr.start();
            }

            else if(kind == ENTRY_DELETE)
            {
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Delete, tcpm, udpm, 2023,
                    address, directory, boundedBuffer, sync, uploadSync);

                fr.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
