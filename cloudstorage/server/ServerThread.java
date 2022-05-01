package cloudstorage.server;

import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import cloudstorage.server.view.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;

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
    public ServerUI ui;
    public SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
    public Date date = new Date(System.currentTimeMillis());
    public String timestamp = formatter.format(date);

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID, List<ClientData> cd, ServerUI u)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        clients = cd;
        ui = u;
    }

    public void run()
    {
        bb = new BoundedBuffer(1);
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);
        sm = new SQLManager();

        sm.setDBConnection();
        
        String action = tcpm.receiveMessageFromClient(1000);
        String fileName = "";

        if(action.equals("quit"))
        {
            return;
        }

        while(true)
        {
            fileName = tcpm.receiveMessageFromClient(1000);

            ui.textfield1.append(" [" + timestamp + "] Active Clients: " + clients.size() + "\n");
            ui.textfield1.append(" [" + timestamp + "] Thread " + ID + " performing " + action + " on " + fileName + "\n");
            System.out.printf("Active Clients: %d\n", clients.size());
            System.out.printf("Thread %d peforming %s\n", ID, action);

            

            switch(action)
            {
                case "upload":
                    uploadFile(fileName);
                    break;
                case "delete":
                    deleteFile(fileName);
                    break;
            }

            ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

            while(files.get(fileName) == null)
            {
                downloadFile(fileName, files);
            }

            action = tcpm.receiveMessageFromClient(1000);

            if(action.equals("quit"))
            {
                sm.closeConnection();
                return;
            }

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

    synchronized public void downloadFile(String fileName, ConcurrentHashMap<String, FileData> files)
    {
        try
        {
            if(files.get(fileName) != null)
            {
                System.out.println("Test");
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
                    fileName, fileSize, numPackets, ui);

                rt.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
