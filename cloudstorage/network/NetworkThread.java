package cloudstorage.network;

import cloudstorage.enums.*;
import java.net.*;

public class NetworkThread extends Thread
{
    public byte[] data;
    public DatagramSocket udpSocket;
    public DatagramPacket packet;
    public Socket tcpSocket;
    public String message;
    public Protocol protocol;
    public NetworkAction action;
    public TCPManager tcpm;
    public UDPManager udpm;
    public boolean isServer;

    public NetworkThread(Socket s, Protocol p, NetworkAction na)
    {
        tcpSocket = s;
        protocol = p;
        action = na;
    }

    public NetworkThread(Socket s, Protocol p, NetworkAction na, boolean is)
    {
        tcpSocket = s;
        protocol = p;
        action = na;
        isServer = is;
    }

    public NetworkThread(DatagramSocket s, Protocol p, NetworkAction na, boolean is)
    {
        udpSocket = s;
        protocol = p;
        action = na;
        isServer = is;
    }

    public void setData(byte[] d)
    {
        data = d;
        notify();
    }

    public void setMessage(String m)
    {
        message = m;
        notify();
    }

    public void run()
    {
        /*
        while(true)
        {
            if(newMessage)
            synchronized(this)
            {

            }

            if(protocol == Protocol.TCP)
            {
                tcpm = new TCPManager(tcpSocket);
    
                switch(action)
                {
                    case Send:
                        sendTCP(isServer);
                        break;
                    case Receive:
                        receiveTCP(isServer);
                        break;
                }
            }
    
            else
            {
                udpm = new UDPManager(udpSocket);
    
                switch(action)
                {
                    case Send:
                        sendUDP(isServer);
                        packet = null;
                        break;
                    case Receive:
                        receiveUDP(isServer)
                        break;
                }
            }                
        }*/
   }

   /*synchronized public void receiveTCP(boolean isServer)
   {
       if(isServer)
       {
           sendPacketToClient(packet, InetAddress clientAddress, int clientPort, int timeout)
       }

       else
       {
           udpm.sendPacketToServer(packet, packet.getAddress(), packet.getPort(), 5000);
       }
   }

   synchronized public void sendTCP(boolean isServer)
   {

   }

   synchronized public void receiveUDP(boolean isServer)
   {

   }

   synchronized public void sendUDP(boolean isServer)
   {
       if(isServer)
       {
           sendPacketToClient(packet, InetAddress clientAddress, int clientPort, int timeout)
       }

       else
       {
           udpm.sendPacketToServer(packet, packet.getAddress(), packet.getPort(), 5000);
       }
   }*/
}
