package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
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
        System.out.printf("CURR_FILE: %s\n", fileData.getFileName());
        System.out.printf("TOKEN: %s\n", token);
        while(!token.equals("") && !fileData.getFileName().equals(token))
        {
            try
            {
                System.out.printf("%s is waiting\n", fileData.getFileName());
                wait();
                Thread.sleep(2000);
            }

            catch(Exception e)
            {

            }
        }

        token = fileData.getFileName();

        // Split file into blocks
        fileData.createSegments(fileData.getData(), 1024 * 1024 * 4, Segment.Block);

        List<byte[]> blocksCreated = fileData.getBlocks();

        tcpm.sendMessageToServer(String.format("upload/%s/%d/%d", fileData.getFileName(), 
            fileData.getFileSize(), blocksCreated.size()), 2000);

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            sync.checkIfPaused();
            
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            tcpm.sendMessageToServer(String.valueOf(packetsCreated.size()), 1000);

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

        token = "";

        try
        {
            notify();
        }

        catch(Exception e)
        {

        }
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
}
