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
    public HashMap<String, FileData> filesInDirectory;
    public Path synchronizedDirectory;
    public String directory;
    public String fileName;
    public Synchronizer downloadSync;
    public Synchronizer uploadSync;
    public WatchEvent.Kind<?> kind;

    public EventProcessor(String fn, Synchronizer ds, Synchronizer us, String d, Path sd, WatchEvent.Kind<?> k,
        FileController fc, HashMap<String, FileData> fid)
    {
        fileName = fn;
        downloadSync = ds;
        uploadSync = us;
        directory = d;
        synchronizedDirectory = sd;
        kind = k;
        controller = fc;
        filesInDirectory = fid;
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

            // Check to see what kind of event was detected and process data accordingly.
            if(kind == ENTRY_CREATE)
            {
                // Get the byte data for the file being processed.
                byte[] sendData = Files.readAllBytes(Paths.get(directory).toAbsolutePath().resolve(fileName));

                FileData fileData = new FileData(sendData, fileName, sendData.length);
                
                fileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);

                fileData.setUnmodifiedBlocks(fileData.getBlocks());
                
                // Update Hashmap for any modified file or created file before running threads
                filesInDirectory.put(fileName, fileData);
                
                // If the event is a create event begin "upload" synchronization without processing for
                // delta sync.
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Upload, directory, controller,
                    filesInDirectory);

                // run thread
                fr.start();
                fr.join();
            }

            else if (kind == ENTRY_MODIFY)
            {
                // Get the byte data for the file being processed.                
                byte[] sendData = Files.readAllBytes(Paths.get(directory).toAbsolutePath().resolve(fileName));

                FileData fileData = filesInDirectory.get(fileName);
                
                fileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);

                boolean fileChanged = fileData.setDeltaSyncBlocks();

                // If the file is the same, do not create a new read thread. Simply remove the block
                // from the file and return.
                if(!fileChanged)
                {
                    uploadSync.blockedFiles.replace(fileName, false);
                    return;
                }
                
                if(filesInDirectory.containsKey(fileName))
                {
                    filesInDirectory.remove(fileName);
                }
            
                //Update Hashmap for any modified file or created file before running threads
                filesInDirectory.put(fileName, fileData);

                // If the event is a create event begin "upload" synchronization with processing for
                // delta sync.
                FileReader fr = new FileReader(fileName, SystemAction.Upload, directory, controller,
                    filesInDirectory);

                // run thread
                fr.start();
                fr.join();
            }

            else if(kind == ENTRY_DELETE)
            {
                // If the event is a delete event begin "delete" synchronization.
                FileReader fr = new FileReader(fileName.toString(), SystemAction.Delete, directory, controller,
                    filesInDirectory);

                if(filesInDirectory.containsKey(fileName))
                {
                    filesInDirectory.remove(fileName);
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
