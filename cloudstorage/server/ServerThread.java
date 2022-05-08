package cloudstorage.server;

import cloudstorage.control.*;
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
    public List<ClientData> clients;
    public ServerUI ui;
    public Socket tcpSocket;
    public SQLManager sm;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int bufferSize;
    public int ID;
    public final int blockSize = 1024 * 1024 * 4;

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
        udpm = new UDPManager(udpSocket);

        sm.setDBConnection();

        BoundedBuffer bb = new BoundedBuffer(1, false, false);

        Synchronizer sync = new Synchronizer(false);

        /*
        ConcurrentHashMap<String, FileData> filesInServer = sm.selectAllFiles();

        DataController dc = new DataController(tcpm, udpm, clients.get(ID - 1).getAddress(Protocol.TCP), 
            clients.get(ID - 1).getPort(Protocol.TCP), clients.get(ID - 1).getAddress(Protocol.UDP),
            clients.get(ID - 1).getPort(Protocol.UDP), bb, ui, ID, sm);

        ServerController sc = new ServerController(tcpm, udpm, sm, ui, bb, dc, buffer, ID);

        tcpm.sendMessageToClient(String.valueOf(filesInServer.size()), 1000);

        if(filesInServer.size() > 0)
        {
            for(String i : filesInServer.keySet())
            {
                ServerReceiver sr = new ServerReceiver(ID, "download", sm, clients, ui, bb, sc);
            }
        }
        */

        while(true)
        {
            String message = tcpm.receiveMessageFromClient(1000);

            String[] components = message.split("/");

            if(!components[0].equals("delete"))
            {
                while(sync.getIsSending())
                {
                    tcpm.sendMessageToClient("wait", 5000);
                }
    
                tcpm.sendMessageToClient("ready", 1000);

                message = tcpm.receiveMessageFromClient(1000);

                components = message.split("/");
            }

            bb = new BoundedBuffer(1, false, false);

            ServerController sc = null;

            DataController dc = new DataController(tcpm, udpm, clients.get(ID - 1).getAddress(Protocol.TCP), 
                clients.get(ID - 1).getPort(Protocol.TCP), clients.get(ID - 1).getAddress(Protocol.UDP),
                clients.get(ID - 1).getPort(Protocol.UDP), bb, ui, ID, sm, sync);

            if(components[0].equals("delete"))
            {
                sc = new ServerController(tcpm, sm, ui, components, bb, dc, buffer, ID);
            }

            else
            {
                sc = new ServerController(tcpm, udpm, sm, ui, components, bb, dc, buffer, ID);
            }

            ui.appendToLog(String.format("Active Clients: %d", clients.size()));

            ServerReceiver sr = new ServerReceiver(ID, components.clone(), sm, clients, ui, bb, sc, dc);

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
