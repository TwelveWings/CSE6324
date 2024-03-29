package cloudstorage.network;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;

public class SendThread extends Thread
{
    public BoundedBuffer boundedBuffer;
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

    public SendThread(UDPManager udp, List<byte[]> dp, ConnectionType ct, Protocol proto, int p, InetAddress a,
        BoundedBuffer bb)
    {
        udpm = udp;
        packets = dp;
        threadType = ct;
        sendProtocol = proto;
        port = p;
        address = a;
        boundedBuffer = bb;
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

  /*
     * \brief sendTCP
     * 
     * Sends a TCP command.
     * 
     * \param tcpm is the TCPManager instance being used.
     * \param threadType is to determine if the request is coming from a client or server.
     * \param sendMessage is the message being sent.
    */
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

  /*
     * \brief sendUDP
     * 
     * Sends a UDP command.
     * 
     * \param udpm is the UDPManager instance being used.
     * \param threadType is to determine if the request is coming from a client or server.
     * \param sendPackets is the packets being sent.
    */
    public synchronized void sendUDP(UDPManager udpm, ConnectionType threadType, List<byte[]> sendPackets)
    {

        byte[] packet = boundedBuffer.withdraw();

        udpm.sendPacket(packet, address, port, 75);
    }
}
