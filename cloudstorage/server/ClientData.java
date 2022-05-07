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

    /*
     * \brief getClientID
     * 
     * Retrieves the ID of the client.
     * 
     * Returns the int value of clientID.
    */
    public int getClientID()
    {
        return clientID;
    }

    /*
     * \brief getPort
     * 
     * Retrieves the TCP port of the client.
     * 
     * Returns the int value of port
    */
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

    /*
     * \brief getAddress
     * 
     * Retrieves the InetAddress of the client.
     * 
     * Returns the InetAddress value of address.
    */
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

    /*
     * \brief setProcessRequest
     * 
     * Assigns a value to the object's processRequest variable.
     * 
     * \param pr is the new boolean value being assigned to processRequest.
    */
    public void setProcessRequest(boolean pr)
    {
        processRequest = pr;
    }

    /*
     * \brief getProcessRequest
     * 
     * Retrieves whether a request is being processed.
     * 
     * Returns the boolean value of processRequest.
    */
    public boolean getProcessRequest()
    {
        return processRequest;
    }

    /*
     * \brief setClientAction
     * 
     * Assigns a value to the object's clientAction variable.
     * 
     * \param ca is the new String value being assigned to clientAction.
    */
    public void setClientAction(String ca)
    {
        clientAction = ca;
    }

    /*
     * \brief getClientAction
     * 
     * Retrieves what action the client is taking.
     * 
     * Returns the String value of the action.
    */
    public String getClientAction()
    {
        return clientAction;
    }

    /*
     * \brief setClientFile
     * 
     * Assigns a value to the object's clientFile variable.
     * 
     * \param cf is the new String value being assigned to clientFile.
    */
    public void setClientFile(String cf)
    {
        clientFile = cf;
    }

    /*
     * \brief getClientFIle
     * 
     * Retrieves the client file
     * 
     * Returns the String value of clientFile.
    */
    public String getClientFile()
    {
        return clientFile;
    }

    /*
     * \brief applyUDPInfo
     * 
     * Updates the UDP information for a data controller. This is so data get sent to the correct client.
    */
    public void applyUDPInfo(DataController dc)
    {
        dc.setUDPAddress(udpAddress);
        dc.setUDPPort(udpPort);
    }

    /*
     * \brief applyTCPInfo
     * 
     * Updates the TCP information for a data controller. This is so commands get sent to the correct
     * client.
    */
    public void applyTCPManager(DataController dc)
    {
        dc.setTCPManager(tcpm);
    }
}
