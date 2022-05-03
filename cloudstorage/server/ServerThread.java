package cloudstorage.server;

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
        
        while(true)
        {
            System.out.printf("Active Clients: %d\n", clients.size());

            String action = tcpm.receiveMessageFromClient(1000);
            String fileName = tcpm.receiveMessageFromClient(1000);

            ServerReceiver sr = new ServerReceiver(ID, tcpSocket, udpSocket, buffer, bufferSize, action, fileName, sm, clients);
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
