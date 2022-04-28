package cloudstorage.network;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;

public class ReceiveThread extends Thread
{
    public byte[] buffer;
    public ConnectionType threadType;
    public Protocol receiveProtocol;
    public String message;
    public DatagramPacket packet;
    public UDPManager udpm;
    public TCPManager tcpm;

    public ReceiveThread(Socket tcp, String m, ConnectionType ct, Protocol p)
    {
        message = m;
        threadType = ct;
        receiveProtocol = p;
    }

    public ReceiveThread(DatagramSocket udp, DatagramPacket dp, ConnectionType ct, Protocol p,
        byte[] b)
    {
        packet = dp;
        threadType = ct;
        receiveProtocol = p;
        buffer = b;
    }

    public void run()
    {
        if(receiveProtocol == Protocol.TCP)
        {
            receiveTCP(tcpm, threadType, message);
        }

        else
        {
            receiveUDP(udpm, threadType, packet);
        }
    }

    public synchronized void receiveTCP(TCPManager tcpm, ConnectionType threadType, String sendMessage)
    {
        if(threadType == ConnectionType.Client)
        {
            tcpm.receiveMessageFromServer(1000);
        }

        else
        {
            tcpm.receiveMessageFromClient(1000);
        }
    }

    public synchronized void receiveUDP(UDPManager udpm, ConnectionType threadType, DatagramPacket sendPacket)
    {
        if(threadType == ConnectionType.Client)
        {
            udpm.receivePacketFromServer(buffer, 1000);
        }

        else
        {
            udpm.receivePacketFromClient(buffer, 1000);
        }
    }
}
