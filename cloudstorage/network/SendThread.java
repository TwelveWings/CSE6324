package cloudstorage.network;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;

public class SendThread extends Thread
{
    public ConnectionType threadType;
    public Protocol sendProtocol;
    public String message;
    List<byte[]> packets;
    public InetAddress address;
    public int port;
    public UDPManager udpm;
    public TCPManager tcpm;

    public SendThread(TCPManager tcp, String m, ConnectionType ct, Protocol proto, int p, InetAddress a)
    {
        tcpm = tcp;
        threadType = ct;
        sendProtocol = proto;
        port = p;
        address = a;
    }

    public SendThread(UDPManager udp, List<byte[]> dp, ConnectionType ct, Protocol proto, int p, InetAddress a)
    {
        udpm = udp;
        packets = dp;
        threadType = ct;
        sendProtocol = proto;
        port = p;
        address = a;
    }

    public void run()
    {
        if(sendProtocol == Protocol.TCP)
        {
            sendTCP(tcpm, threadType, message);
        }

        else
        {
            sendUDP(udpm, threadType, packets);
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

    public synchronized void sendUDP(UDPManager udpm, ConnectionType threadType, List<byte[]> sendPackets)
    {
        if(threadType == ConnectionType.Client)
        {            
            for(int i = 0; i < sendPackets.size(); i++)
            {    
                System.out.printf("SP: %d\n", sendPackets.get(i)[1]);
                udpm.sendPacketToServer(sendPackets.get(i), address, port, 1000);
            }
        }

        else
        {
            for(int i = 0; i < sendPackets.size(); i++)
            {
                udpm.sendPacketToClient(sendPackets.get(i), address, port, 1000);
            }
        }
    }
}
