package cloudstorage.data;

import java.net.*;

public class ClientData 
{
    public InetAddress address;
    public String clientAction;
    public String clientFile;
    public boolean processRequest;
    public int clientID;
    public int port;

    public ClientData(int cid, int p, InetAddress a)
    {
        clientID = cid;
        port = p;
        processRequest = false;
        address = a;
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
}
