package cloudstorage.network;

import cloudstorage.enums.*;
import java.net.*;

public class NetworkThread extends Thread
{
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public DatagramPacket packet;
    public Socket tcpSocket;
    public String message;
    public Protocol protocol;
    public NetworkAction action;
    public TCPManager tcpm;
    public UDPManager udpm;

    public NetworkThread(Socket s, Protocol p, NetworkAction na)
    {
        tcpSocket = s;
        protocol = p;
        action = na;
    }

    public NetworkThread(Socket s, Protocol p, NetworkAction na, String m)
    {
        tcpSocket = s;
        protocol = p;
        action = na;
        message = m;
    }

    public NetworkThread(DatagramSocket s, Protocol p, NetworkAction na, byte[] b)
    {
        udpSocket = s;
        protocol = p;
        action = na;
        buffer = b;
    }

    public void run()
    {
        /*
        if(protocol == Protocol.TCP)
        {
            tcpm = new TCPManager(tcpSocket);

            switch(action)
            {
                case Create:
                    tcpm.createPa
                    break;
                case Send:
                    break;
                case Receive:
                    break;
            }
        }

        else
        {
            udpm = new UDPManager(udpSocket);

            switch(action)
            {
                case Create:
                    packet = udpm.createDatagram(buffer);
                    break;
                case Send:
                    udpm.sendPacketToServer(packet, packet.getAddress(), packet.getPort(), 5000);
                    packet = null;
                    break;
                case Receive:
                    break;
            }
        }*/
    }
}
