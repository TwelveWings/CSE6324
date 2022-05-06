package cloudstorage.client;

import cloudstorage.enums.*;
import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.net.*;
import java.nio.file.*;
import java.util.HashMap;

public class EventProcessor extends Thread
{
    public FileController controller;
    public Path synchronizedDirectory;
    public String directory;
    public String fileName;
    public Synchronizer downloadSync;
    public Synchronizer uploadSync;
    public WatchEvent.Kind<?> kind;
    public HashMap<String, FileData> unmodifiedFilesInDirectory;

    public EventProcessor(String fn, Synchronizer ds, Synchronizer us, String d, Path sd, WatchEvent.Kind<?> k, FileController fc, HashMap<String, FileData> ufid)
    {
        fileName = fn;
        downloadSync = ds;
        uploadSync = us;
        directory = d;
        synchronizedDirectory = sd;
        kind = k;
        controller = fc;
        unmodifiedFilesInDirectory = ufid;
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
            if(kind == ENTRY_CREATE)
            {
                FileReader fr = new FileReader(fileName, SystemAction.Upload, directory, controller, false);
            
                //Update Hashmap for any modified file or created file before running threads
                
                byte[] sendData = Files.readAllBytes(Paths.get(directory).toAbsolutePath().resolve(fileName));

                FileData fileData = new FileData(sendData, fileName, sendData.length);
                
                fileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);
                
                unmodifiedFilesInDirectory.put(fileName, fileData);

                // run thread
                fr.start();
                fr.join();
            }

            else if (kind == ENTRY_MODIFY)
            {
                System.out.println("getting file in unmodified hashmap:");
                System.out.println(unmodifiedFilesInDirectory.get(fileName));

                FileReader fr = new FileReader(fileName, SystemAction.Upload, directory, controller, true, unmodifiedFilesInDirectory.get(fileName));
            
                //Update Hashmap for any modified file or created file before running threads
                
                byte[] sendData = Files.readAllBytes(Paths.get(directory).toAbsolutePath().resolve(fileName));

                FileData fileData = new FileData(sendData, fileName, sendData.length);
                
                fileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);
                
                if(unmodifiedFilesInDirectory.containsKey(fileName))
                {
                    unmodifiedFilesInDirectory.remove(fileName);
                }
                
                unmodifiedFilesInDirectory.put(fileName, fileData);

                // run thread
                fr.start();
                fr.join();
            }

            else if(kind == ENTRY_DELETE)
            {
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Delete, directory, controller, false);

                if(unmodifiedFilesInDirectory.containsKey(fileName))
                {
                    unmodifiedFilesInDirectory.remove(fileName);
                }

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
