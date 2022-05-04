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
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Socket tcpSocket;
    public SQLManager sm;
    public TCPManager tcpm;
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
        sm = new SQLManager();
        tcpm = new TCPManager(tcpSocket);

        sm.setDBConnection();

        BoundedBuffer bb = new BoundedBuffer(1, false);

        ConcurrentHashMap<String, FileData> filesInServer = sm.selectAllFiles();

        tcpm.sendMessageToClient(String.valueOf(filesInServer.size()), 1000);

        if(filesInServer.size() > 0)
        {
            for(String i : filesInServer.keySet())
            {
                clients.get(ID - 1).synchronizeWithClients(filesInServer.get(i).getFileName(), "download", 
                    sm, clients.get(ID - 1), bb);
            }
        }

        while(true)
        {
            System.out.printf("Active Clients: %d\n", clients.size());

            String action = tcpm.receiveMessageFromClient(1000);
            String fileName = tcpm.receiveMessageFromClient(1000);

            ServerReceiver sr = new ServerReceiver(ID, tcpSocket, udpSocket, buffer, bufferSize, action,
                fileName, sm, clients);

            sr.start();

            try
            {
                sr.join();
            }

            catch(Exception e)
            {

            }
        }



    }
}
