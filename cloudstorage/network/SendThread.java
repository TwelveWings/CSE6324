package cloudstorage.network;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;

public class SendThread extends Thread
{
    public ConnectionType threadType;
    public Protocol sendProtocol;
    public String message;
    public byte[] packet;
    public InetAddress address;
    public int port;
    public UDPManager udpm;
    public TCPManager tcpm;

    public SendThread(Socket tcp, String m, ConnectionType ct, Protocol p)
    {
        message = m;
        threadType = ct;
        sendProtocol = p;
    }

    public SendThread(DatagramSocket udp, byte[] dp, ConnectionType ct, Protocol p)
    {
        packet = dp;
        threadType = ct;
        sendProtocol = p;
    }

    public void run()
    {
        if(sendProtocol == Protocol.TCP)
        {
            sendTCP(tcpm, threadType, message);
        }

        else
        {
            sendUDP(udpm, threadType, packet);
        }
    }

    public synchronized void sendTCP(TCPManager tcpm, ConnectionType threadType, String sendMessage)
    {
        if(threadType == ConnectionType.Client)
        {
            tcpm.sendMessageToServer(sendMessage, 1000);
        }

        else
        {
            tcpm.sendMessageToClient(sendMessage, 1000);
        }
    }

    public synchronized void sendUDP(UDPManager udpm, ConnectionType threadType, byte[] sendPacket)
    {
        if(threadType == ConnectionType.Client)
        {
            udpm.sendPacketToServer(sendPacket, address, port, 1000);
        }

        else
        {
            udpm.sendPacketToClient(sendPacket, address, port, 1000);
        }
    }
}
