package cloudstorage.server;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.util.concurrent.*;

public class ClientData 
{
    public DataController controller;
    public InetAddress address;
    public InetAddress udpAddress;
    public String clientAction;
    public String clientFile;
    public boolean processRequest;
    public int clientID;
    public int port;
    public int udpPort;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientData(int cid, int p, InetAddress a, int udpp, InetAddress udpa, Socket tcp, DatagramSocket udp)
    {
        clientID = cid;
        port = p;
        udpPort = udpp;
        processRequest = false;
        address = a;
        udpAddress = udpa;
        tcpm = new TCPManager(tcp);
        udpm = new UDPManager(udp);
    }

    public int getClientID()
    {
        return clientID;
    }


    public int getPort(Protocol proto)
    {
        if(proto == Protocol.UDP)
        {
            return udpPort;
        }

        else
        {
            return port;
        }
    }

    public InetAddress getAddress(Protocol proto)
    {
        if(proto == Protocol.UDP)
        {
            return udpAddress;
        }

        else
        {
            return address;
        }
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

    public void applyUDPInfo(DataController dc)
    {
        dc.setUDPAddress(udpAddress);
        dc.setUDPPort(udpPort);
    }

    public void applyTCPManager(DataController dc)
    {
        dc.setTCPManager(tcpm);
    }
}
