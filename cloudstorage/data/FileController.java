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
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String fileName;
    public String timestamp;
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

    synchronized public void upload(FileData fileData)
    {
        StringBuilder sb = new StringBuilder();

        // Split file into blocks
        fileData.createSegments(fileData.getData(), 1024 * 1024 * 4, Segment.Block);

        List<byte[]> blocksCreated = fileData.getBlocks();

        sb.append(String.format("upload/%s/%d/%d", fileData.getFileName(), 
        fileData.getFileSize(), blocksCreated.size()));

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);
            sb.append("/" + String.valueOf(fileData.getPackets().size()));           
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

    synchronized public void delete(FileData fileData)
    {
        while(!token.equals("") && !fileData.getFileName().equals(token))
        {
            try
            {
                wait();
                Thread.sleep(2000);
            }

            catch(Exception e)
            {

            }
        }

        token = fileData.getFileName();   

        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        ui.appendToLog(String.format("%s deleted. Updating server.", fileData.getFileName()));

        tcpm.sendMessageToServer(String.format("delete/%s", fileData.getFileName()), 1000);

        ui.appendToLog("Complete.");

        uploadSync.blockedFiles.replace(fileData.getFileName(), false);

        token = "";

        try
        {
            notify();
        }
        
        catch(Exception e)
        {

        }
    }

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
