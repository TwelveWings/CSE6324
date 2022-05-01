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

    public void downloadToClient(String fileName, SQLManager sm, ClientData client, BoundedBuffer bb)
    {
        ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

        try
        {
            if(files.get(fileName) != null)
            {
                DBReader dbr = new DBReader(files.get(fileName).data, fileName, files.get(fileName).fileSize, tcpm, udpm, client.getPort(), 
                    client.getAddress(), SystemAction.Download, bb);
                dbr.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
