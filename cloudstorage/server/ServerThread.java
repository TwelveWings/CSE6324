package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class ServerThread extends Thread
{
    public BoundedBuffer bb;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Socket tcpSocket;
    public SQLManager sm;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int ID;
    public final int blockSize = 1024 * 1024 * 4;
    public int bufferSize;
    public List<ClientData> clients;

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID, List<ClientData> cd)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        clients = cd;
    }

    public void run()
    {
        bb = new BoundedBuffer(1, false);
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);
        sm = new SQLManager();

        ClientData client = clients.get(ID - 1);

        sm.setDBConnection();
        
        String action = tcpm.receiveMessageFromClient(1000);
        String fileName = "";

        if(action.equals("quit"))
        {
            return;
        }

        while(true)
        {
            System.out.printf("Active Clients: %d\n", clients.size());
            System.out.printf("Thread %d peforming %s\n", ID, action);

            fileName = tcpm.receiveMessageFromClient(1000);

            switch(action)
            {
                case "upload":
                    uploadFile(fileName);
                    break;
                case "delete":
                    deleteFile(fileName);
                    break;
            }
 
            while(!bb.getFileUploaded())
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

            if(clients.size() > 1)
            {
                for(int i = 0; i < clients.size(); i++)
                {
                    if(clients.get(i).getClientID() == ID)
                    {
                        continue;
                    }

                    clients.get(i).synchronizeWithClients(fileName, action, sm, clients.get(i), bb);
                }
            }

            action  = tcpm.receiveMessageFromClient(1000);

            System.out.println(action);

            Arrays.fill(buffer, (byte)0);
        }
    }

    synchronized public void deleteFile(String fileName)
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

    synchronized public void uploadFile(String fileName)
    {
        try
        {
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

            int numBlocks = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

            //JOptionPane.showMessageDialog(null, numPackets);

            List<byte[]> data = new ArrayList<byte[]>();

            for(int i = 0; i < numBlocks; i++)
            {
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

                byte[][] packets = new byte[numPackets][];

                for(int j = 0; j < numPackets; j++)
                {
                    ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Server, Protocol.UDP, buffer, data, packets,
                        fileName, fileSize, numBlocks, numPackets, bb);

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
