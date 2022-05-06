package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class ServerThread extends Thread
{
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Date date = new Date(System.currentTimeMillis());
    public List<ClientData> clients;
    public ServerUI ui;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public Socket tcpSocket;
    public SQLManager sm;
    public String timestamp = formatter.format(date);
    public TCPManager tcpm;
    public int ID;
    public final int blockSize = 1024 * 1024 * 4;
    public int bufferSize;

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID, List<ClientData> cd,
        ServerUI u)
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
        sm = new SQLManager();
        tcpm = new TCPManager(tcpSocket);

        sm.setDBConnection();

        BoundedBuffer bb = new BoundedBuffer(1, false, false);

        ConcurrentHashMap<String, FileData> filesInServer = sm.selectAllFiles();

        tcpm.sendMessageToClient(String.valueOf(filesInServer.size()), 1000);

        /*
        if(filesInServer.size() > 0)
        {
            for(String i : filesInServer.keySet())
            {
                clients.get(ID - 1).synchronizeWithClients(filesInServer.get(i).getFileName(), "download", 
                    sm, clients.get(ID - 1), bb, ui);
            }
        }*/

        while(true)
        {
            String message = tcpm.receiveMessageFromClient(1000);

            System.out.printf("MESSAGE RECEIVED FROM CLIENT%d: %s", ID, message);

            String[] components = message.split("/");

            ui.appendToLog(String.format("Active Clients: %d", clients.size()));

            ServerReceiver sr = new ServerReceiver(ID, tcpSocket, udpSocket, buffer, bufferSize, components.clone(), sm,
                clients, ui);

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
