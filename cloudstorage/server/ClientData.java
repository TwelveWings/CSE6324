package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.concurrent.*;

public class ClientData 
{
    public InetAddress address;
    public String clientAction;
    public String clientFile;
    public boolean processRequest;
    public int clientID;
    public int port;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientData(int cid, int p, InetAddress a, Socket tcp, DatagramSocket udp)
    {
        clientID = cid;
        port = p;
        processRequest = false;
        address = a;
        tcpm = new TCPManager(tcp);
        udpm = new UDPManager(udp);
    }

    public int getClientID()
    {
        return clientID;
    }


    public int getPort()
    {
        return port;
    }

    public InetAddress getAddress()
    {
        return address;
    }    

    public void setProcessRequest(boolean pr)
    {
        processRequest = pr;
    }

    public boolean getProcessRequest()
    {
        return processRequest;
    }

    public void setClientAction(String ca)
    {
        clientAction = ca;
    }

    public String getClientAction()
    {
        return clientAction;
    }

    public void setClientFile(String cf)
    {
        clientFile = cf;
    }

    public String getClientFile()
    {
        return clientFile;
    }

    synchronized public void synchronizeWithClients(String fileName, String action, SQLManager sm,
        ClientData client, BoundedBuffer bb)
    {
        System.out.printf("SYNCHRONIZE WITH CLIENTS: %s\n", fileName);
        ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

        SystemAction command = (action.equals("delete")) ? SystemAction.Delete : SystemAction.Download;

        try
        {
            if(files.get(fileName) != null)
            {
                DBReader dbr = new DBReader(files.get(fileName).data, fileName, files.get(fileName).fileSize,
                    tcpm, udpm, client.getPort(), client.getAddress(), command, bb);

                dbr.start();
            }

            else if(command == SystemAction.Delete)
            {
                DBReader dbr = new DBReader(fileName, tcpm, udpm, client.getPort(), client.getAddress(), command, bb);

                dbr.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
