package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataController 
{
    public BoundedBuffer boundedBuffer;
    public Date date = new Date(System.currentTimeMillis());
    public InetAddress targetAddress;
    public ServerUI ui;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String timestamp = formatter.format(date);
    public TCPManager tcpm;
    public UDPManager udpm;
    public int clientID;
    public int targetPort;
    public volatile String token;

    public DataController(TCPManager tcp, UDPManager udp, InetAddress a, int p, BoundedBuffer bb, 
        ServerUI u, int ID)
    {
        tcpm = tcp;
        udpm = udp;
        targetAddress = a;
        targetPort = p;
        boundedBuffer = bb;
        ui = u;
        token = "";
        clientID = ID;
    }

    synchronized public void download(FileData fileData)
    {
        byte[] buffer = new byte[65507];

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

        tcpm.sendMessageToClient("download", 2000);
        tcpm.sendMessageToClient(fileData.getFileName(), 2000);
        tcpm.sendMessageToClient(String.valueOf(fileData.getFileSize()), 2000);
        tcpm.sendMessageToServer(String.valueOf(blocksCreated.size()), 2000);

        DatagramPacket connector = udpm.receiveDatagramPacket(buffer, 2000);

        ui.textfield1.append(" [" + timestamp + "] Transmitting data to Client " + String.valueOf(clientID) + 
            "...\n");

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            tcpm.sendMessageToClient(String.valueOf(packetsCreated.size()), 1000);

            targetPort = connector.getPort();
            targetAddress = connector.getAddress();

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                boundedBuffer.deposit(packetsCreated.get(j));

                // Send the packet
                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Server,
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
    }

    synchronized public void delete(FileData fileData)
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

        ui.textfield1.append(" [" + timestamp + "] " + fileData.getFileName() + " deleted. Updating " +
            "Client " + String.valueOf(clientID) + "...\n");

        tcpm.sendMessageToClient("delete", 1000);

        try
        {
            // Send file name to delete on server.
            tcpm.sendMessageToClient(fileData.getFileName(), 1000);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        ui.textfield1.append(" [" + timestamp + "] Complete.\n");

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
