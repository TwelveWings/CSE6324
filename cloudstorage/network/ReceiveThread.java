package cloudstorage.network;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;

public class ReceiveThread extends Thread
{
    public byte[][] combinedPackets;
    public byte[] buffer;
    public BoundedBuffer boundedBuffer;
    public ConnectionType threadType;
    public Protocol receiveProtocol;
    public String fileName;
    public String directory;
    public Synchronizer sync;
    public UDPManager udpm;
    public TCPManager tcpm;
    public int numPackets;
    public int fileSize;

    public ReceiveThread(TCPManager tcp, ConnectionType ct, Protocol p)
    {
        tcpm = tcp;
        threadType = ct;
        receiveProtocol = p;
    }

    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        byte[][] cp, String fn, int fs, int np, BoundedBuffer bb)
    {
        combinedPackets = cp;
        udpm = udp;
        threadType = ct;
        receiveProtocol = p;
        buffer = b;
        fileName = fn;
        fileSize = fs;
        numPackets = np;
        boundedBuffer = bb;
    }


    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        byte[][] cp, String fn, int fs, int np, BoundedBuffer bb, String dir, Synchronizer s)
    {
        combinedPackets = cp;
        udpm = udp;
        threadType = ct;
        receiveProtocol = p;
        buffer = b;
        fileName = fn;
        sync = s;
        fileSize = fs;
        numPackets = np;
        boundedBuffer = bb;
        directory = dir;
    }

    public void run()
    {
        if(receiveProtocol == Protocol.TCP)
        {
            receiveTCP(tcpm, threadType);
        }

        else
        {
            receiveUDP(udpm, threadType);

        }
    }

    public synchronized void receiveTCP(TCPManager tcpm, ConnectionType threadType)
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

    public synchronized void receiveUDP(UDPManager udpm, ConnectionType threadType)
    {        
        FileData fd = new FileData();
        byte[] packet = udpm.receivePacket(buffer, 1000);

        //System.out.printf("RP: %d\n", packet[1]);
    
        int identifier = packet[1];
        int scale = packet[0];

        packet = fd.stripIdentifier(packet);

        if(fileSize % buffer.length > 0 && identifier == numPackets - 1)
        {
            packet = fd.stripPadding(packet, fileSize % (buffer.length - 2));
        }

        boundedBuffer.deposit(packet);

        if(threadType == ConnectionType.Client)
        {
            FileWriter writer = new FileWriter(combinedPackets, buffer, fileName, fileSize, identifier, scale, numPackets, boundedBuffer, directory, sync);
            writer.start();
        }

        else
        {
            DBWriter writer = new DBWriter(combinedPackets, buffer, fileName, fileSize, identifier, scale, numPackets, boundedBuffer);
            writer.start();
        }
    }
}
