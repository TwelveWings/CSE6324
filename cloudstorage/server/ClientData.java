package cloudstorage.server;

import java.net.*;

public class ClientData 
{
    public int port;
    public InetAddress address;

    public ClientData(int p, InetAddress a)
    {
        port = p;
        address = a;
    }

    public int getPort()
    {
        return port;
    }

    public InetAddress getAddress()
    {
        return address;
    }
    
}
