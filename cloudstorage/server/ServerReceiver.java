package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerReceiver extends Thread
{
    public BoundedBuffer bb;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Date date = new Date(System.currentTimeMillis());
    public ServerUI ui;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public Socket tcpSocket;
    public List<ClientData> clients;
    public SQLManager sm;
    public String[] components;
    public String action;
    public String fileName;
    public String timestamp = formatter.format(date);
    public TCPManager tcpm;
    public UDPManager udpm;
    public int bufferSize;
    public int ID;

    public ServerReceiver(int tID, Socket tcp, DatagramSocket udp, byte[] b, int bs, String[] comp, 
        SQLManager sql, List<ClientData> c, ServerUI u)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        sm = sql;
        clients = c;
        ui = u;
        components = comp;
    }

    public void run()
    {
        action = components[0];
        fileName = components[1];
        
        bb = new BoundedBuffer(1, false, false);
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);

        ui.textfield1.append(" [" + timestamp + "] Client " + ID + " performing " + action + " on " + fileName + "\n");

        switch(action)
        {
            case "upload":
                bb.setFileUploading(true);
                uploadFile(fileName);
                break;
            case "delete":
                deleteFile(fileName);
                break;
        }

        while(bb.getFileUploading() && action.equals("upload"))
        {
            try
            {
                System.out.println("Waiting for upload to complete...");
                Thread.sleep(3000);
            }

            catch(InterruptedException e)
            {

            }
        }

        // If there is more than one client active, synchronize all other clients.
        if(clients.size() > 1)
        {
            for(int i = 0; i < clients.size(); i++)
            {
                if(clients.get(i).getClientID() == ID)
                {
                    continue;
                }

                clients.get(i).synchronizeWithClients(fileName, action, sm, clients.get(i), bb, ui);
            }
        }

        Arrays.fill(buffer, (byte)0);
    }

    public void deleteFile(String fileName)
    {
        try
        {
            sm.deleteFile(fileName);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void uploadFile(String fileName)
    {
        try
        {
            int fileSize = Integer.valueOf(components[2]);

            int numBlocks = Integer.valueOf(components[3]);

            List<byte[]> data = new ArrayList<byte[]>();

            ui.textfield1.append(" [" + timestamp + "] Receiving data from Client " + String.valueOf(ID) +
                "...\n");

            for(int i = 0; i < numBlocks; i++)
            {
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

                System.out.printf("NUM PACKETS: %d\n", numPackets);

                byte[][] packets = new byte[numPackets][];

                for(int j = 0; j < numPackets; j++)
                {
                    ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Server, Protocol.UDP,
                        buffer, data, packets, fileName, fileSize, numBlocks, numPackets, bb, ui);

                    rt.start();
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
