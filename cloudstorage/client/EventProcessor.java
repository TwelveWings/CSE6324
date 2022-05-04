package cloudstorage.client;

import cloudstorage.enums.*;
import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.net.*;
import java.nio.file.*;

public class EventProcessor extends Thread
{
    public BoundedBuffer boundedBuffer;
    public ClientUI ui;
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
        Synchronizer s, Synchronizer us, String d, Path sd, WatchEvent.Kind<?> k, BoundedBuffer bb,
        ClientUI u)
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
        ui = u;
    }
    
    public void run()
    {
        // Checks to see if a download or upload is currently in progress for a certain file. If so, 
        // prevent any threads for the same file from completing. The downloadSync was added to prevent
        // an infinite upload loop once files were pushed from the server to the client. The uploadSync
        // was added to counteract the OS creating multiple events upon file creation.
        if((downloadSync.blockedFiles.containsKey(fileName) && downloadSync.blockedFiles.get(fileName)) ||
           (uploadSync.blockedFiles.containsKey(fileName) && uploadSync.blockedFiles.get(fileName)))
        {
            System.out.printf("DOWNLOAD BLOCKED: %b\n", downloadSync.blockedFiles.get(fileName));
            System.out.printf("UPLOAD BLOCKED: %b\n", uploadSync.blockedFiles.get(fileName));
            return;
        }

        // If the file has not been added to the blockedFiles, add it and set it to true. Otherwise, if
        // the file exists in blockedFiles, set it to true.
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
                    address, directory, boundedBuffer, sync, uploadSync, ui);

                fr.start();
                fr.join();
            }

            else if(kind == ENTRY_DELETE)
            {
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Delete, tcpm, udpm, 2023,
                    address, directory, boundedBuffer, sync, uploadSync, ui);

                fr.start();
                fr.join();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
