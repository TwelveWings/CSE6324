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
    public Date date;
    public InetAddress targetAddress;
    public ServerUI ui;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String timestamp;
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

    /* 
     * \brief download
     * 
     * Sends the bytes of a file to a client. Bytes are sent in blocks, which are broken into packets
     * before transmission.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
    */
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
        
        tcpm.sendMessageToClient(String.format("download/%s/%d/%d", fileData.getFileName(),
            fileData.getFileSize(), blocksCreated.size()), 2000);

        // A datagram packet must be received from the client in order to establish which port is being
        // used.
        DatagramPacket connector = udpm.receiveDatagramPacket(buffer, 2000);

        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
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

                date = new Date(System.currentTimeMillis());
                timestamp = formatter.format(date);
                ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS : " + 
                    String.valueOf(packetsCreated.get(j)[1] + 1) + " OUT OF " + 
                    String.valueOf(packetsCreated.size()) + "\n");

                date = new Date(System.currentTimeMillis());
                timestamp = formatter.format(date);
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

        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        ui.textfield1.append(" [" + timestamp + "] Data transmission for " + fileData.getFileName() + 
            " to Client complete.\n");
        
        token = "";

        try
        {
            notify();
        }

        catch(Exception e)
        {

        }
    }

    /*
     * \brief delete
     * 
     * Sends the delete command to a client, to delete a file from the local directory.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
    */
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

        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        ui.textfield1.append(" [" + timestamp + "] " + fileData.getFileName() + " deleted. Updating " +
            "Client " + String.valueOf(clientID) + "...\n");

        tcpm.sendMessageToClient(String.format("delete,%s", fileData.getFileName()), 1000);

        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
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
