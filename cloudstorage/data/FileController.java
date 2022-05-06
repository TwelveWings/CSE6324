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
    public Date date = new Date(System.currentTimeMillis());
    public InetAddress targetAddress;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String fileName;
    public String timestamp = formatter.format(date);
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

        tcpm.sendMessageToServer("upload", 2000);
        tcpm.sendMessageToServer(fileData.getFileName(), 2000);
        tcpm.sendMessageToServer(String.valueOf(fileData.deltaSyncStartIndex), 1000);
        tcpm.sendMessageToServer(String.valueOf(fileData.deltaSyncEndIndex), 1000);
        tcpm.sendMessageToServer(String.valueOf(fileData.getFileSize()), 2000);
        tcpm.sendMessageToServer(String.valueOf(blocksCreated.size()), 2000);

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            sync.checkIfPaused();
            
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            tcpm.sendMessageToServer(String.valueOf(packetsCreated.size()), 1000);

            ui.textfield1.append(" [" + timestamp + "] Transmitting data to server...\n");

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                sync.checkIfPaused();

                boundedBuffer.deposit(packetsCreated.get(j));

                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Client, 
                    Protocol.UDP, targetPort, targetAddress, boundedBuffer);
                st.start();

                ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS : " + 
                    String.valueOf(packetsCreated.get(j)[1] + 1) + " OUT OF " + 
                    String.valueOf(packetsCreated.size()) + "\n");

                ui.textfield1.append(" [" + timestamp + "] NUM_BLOCKS : " + 
                    String.valueOf(i + 1) + " OUT OF " + 
                    String.valueOf(blocksCreated.size()) + "\n");

                try
                {
                    st.join();
                }

                catch (Exception e)
                {

                }
            }
        }

        ui.textfield1.append(" [" + timestamp + "] Data transmission for " + fileData.getFileName() + 
            " complete.\n");

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
     
        ui.textfield1.append(" [" + timestamp + "] " + fileData.getFileName() + " deleted. Updating " +
            "server.\n");

        tcpm.sendMessageToServer("delete", 1000);

        try
        {
            // Send file name to delete on server.
            tcpm.sendMessageToServer(fileData.getFileName(), 1000);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        ui.textfield1.append(" [" + timestamp + "] Complete.\n");

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
