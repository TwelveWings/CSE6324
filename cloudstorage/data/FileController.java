package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class FileController 
{
    public BoundedBuffer boundedBuffer;
    public ClientUI ui;
    public Date date;
    public InetAddress targetAddress;
    public String fileName;
    public Synchronizer sync;
    public Synchronizer uploadSync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int targetPort;
    public volatile String token;

    public FileController(TCPManager tcp, UDPManager udp, Synchronizer s, BoundedBuffer bb,
        InetAddress a, int p, ClientUI u)
    {
        tcpm = tcp;
        udpm = udp;
        sync = s;
        boundedBuffer = bb;
        targetAddress = a;
        targetPort = p;
        ui = u;
        token = "";
    }

    public FileController(TCPManager tcp, UDPManager udp, Synchronizer s, Synchronizer us, BoundedBuffer bb,
        InetAddress a, int p, ClientUI u)
    {
        tcpm = tcp;
        udpm = udp;
        sync = s;
        uploadSync = us;
        boundedBuffer = bb;
        targetAddress = a;
        targetPort = p;
        ui = u;
        token = "";
    }

    /* 
     * \brief upload
     * 
     * Sends the bytes of a file to the server. Bytes are sent in blocks, which are broken into packets
     * before transmission.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
    */
    synchronized public void upload(FileData fileData)
    {
        StringBuilder sb = new StringBuilder();

        List<byte[]> blocksCreated = fileData.getBlocks();

        sb.append(String.format("upload/%s/%d/%d", fileData.getFileName(), fileData.getFileSize(), blocksCreated.size()));

        if(fileData.isFileModified())
        {
            List<Integer> changedIndices = fileData.getChanges();

            // Denotes file has been modified
            sb.append("/m");

            for(int i = 0; i < changedIndices.size(); i++)
            {
                sb.append(String.format("/%d", changedIndices.get(i)));
            }

            // Denotes end of modification
            sb.append("/em");
        }

        else
        {
            // Denotes file is unmodified
            sb.append("/u");
        }

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);
            sb.append(String.format("/%d", fileData.getPackets().size()));  
        }

        System.out.printf("BUILT STRING: %s\n", sb.toString());

        tcpm.sendMessageToServer(sb.toString(), 1000);

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            sync.checkIfPaused();
            
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            ui.appendToLog("Transmitting data to server...");

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                sync.checkIfPaused();

                boundedBuffer.deposit(packetsCreated.get(j));

                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Client, 
                    Protocol.UDP, targetPort, targetAddress, boundedBuffer);
                st.start();

                ui.appendToLog(String.format("NUM_PACKETS: %d OUT OF %d", (packetsCreated.get(j)[1] + 1),
                    packetsCreated.size()));

                ui.appendToLog(String.format("NUM_BLOCKS: %d OUT OF %d", (i + 1), blocksCreated.size()));
                try
                {
                    st.join();
                }

                catch (Exception e)
                {

                }
            }
        }

        ui.appendToLog(String.format("Data transmission for %s complete.", fileData.getFileName()));

        uploadSync.blockedFiles.replace(fileData.getFileName(), false);
    }

    /* 
     * \brief delete
     * 
     * Deletes the file in the local directory
     * 
     * \param fileName is the name of the file that will be deleted.
    */
    synchronized public void delete(String fileName)
    {
        ui.appendToLog(String.format("%s deleted. Updating server.", fileName));

        tcpm.sendMessageToServer(String.format("delete/%s", fileName), 1000);

        ui.appendToLog("Complete.");

        uploadSync.blockedFiles.replace(fileName, false);
    }


    /* 
     * \brief download
     * 
     * After receiving the bytes from the sever, the client combines them into a data packet. A FileOutputStream 
     * is used to recombine them. The recombined data is written to a file in the local directory.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
     * \param directory is the directory the file will be written to.
    */
    public void download(FileData fileData, String directory)
    {
        try(FileOutputStream fos = new FileOutputStream(directory + "/" + fileData.getFileName()))
        {
            fos.write(fileData.getData());
        }
       
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        boundedBuffer.setFileDownloading(false);

        ui.appendToLog(String.format("Synchronization complete: %s added/updated!", fileData.getFileName()));
    }   
}
