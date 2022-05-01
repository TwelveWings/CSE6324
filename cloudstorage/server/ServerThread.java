package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

            downloadFile(fileName);

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

    synchronized public void downloadFile(String fileName)
    {
        ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

        System.out.printf("SERVER: %s\n", fileName);
        String x = (files.get(fileName) != null) ? files.get(fileName).fileName : "None";
        
        System.out.println(x);

        try
        {
            if(files.get(fileName) != null)
            {
                for(int i = 0; i < clients.size(); i++)
                {
                    if(clients.get(i).getPort() == tcpSocket.getPort() && clients.get(i).getAddress() == tcpSocket.getInetAddress())
                    {
                        continue;
                    }
                    DBReader dbr = new DBReader(files.get(fileName).data, fileName, files.get(fileName).fileSize, tcpm, udpm, clients.get(i).getPort(), clients.get(i).getAddress(), SystemAction.Download);
                    dbr.start();
                }
            }
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

            // Receive a TCP message indicating the number of UDP packets being sent.
            int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

            byte[][] packets = new byte[numPackets][];

            for(int i = 0; i < numPackets; i++)
            {
                ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Server, Protocol.UDP, buffer, packets,
                    fileName, fileSize, numPackets, bb);

                rt.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
