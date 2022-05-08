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
    public BoundedBuffer boundedBuffer;
    public DatagramSocket udpSocket;
    public DataController dataController;
    public ServerUI ui;
    public List<ClientData> clients;
    public SQLManager sm;
    public String[] components;
    public String action;
    public String fileName;
    public ServerController serverController;
    public int ID;

    public ServerReceiver(int tID, String a, SQLManager sql, List<ClientData> c, ServerUI u,
        BoundedBuffer bb, ServerController sc)
    {
        ID = tID;
        sm = sql;
        clients = c;
        ui = u;
        action = a;
        boundedBuffer = bb;
        serverController = sc;
    }

    public ServerReceiver(int tID, String[] comp, SQLManager sql, List<ClientData> c, ServerUI u,
        BoundedBuffer bb, ServerController sc, DataController dc)
    {
        ID = tID;
        sm = sql;
        clients = c;
        ui = u;
        components = comp;
        boundedBuffer = bb;
        serverController = sc;
        dataController = dc;
        action = "";
    }

    public void run()
    {
        if(action.equals("download"))
        {
            return;
        }

        action = components[0];
        fileName = components[1];
        
        ui.appendToLog(String.format("Client %d performing %s on %s", ID, action, fileName));

        switch(action)
        {
            case "upload":
                boundedBuffer.setFileUploading(true);
                serverController.uploadFile(fileName);
                break;
            case "delete":
                serverController.deleteFile(fileName);
                break;
        }

        while(boundedBuffer.getFileUploading() && action.equals("upload"))
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

                clients.get(i).applyTCPManager(dataController);
                clients.get(i).applyUDPInfo(dataController);
                serverController.synchronizeWithClients(fileName, action, sm, clients.get(i), boundedBuffer, ui, dataController);
            }
        }
    }
}
