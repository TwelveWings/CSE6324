package cloudstorage.network;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.util.*;

public class ReceiveThread extends Thread
{
    public byte[][] combinedPackets;
    public byte[] buffer;
    public BoundedBuffer boundedBuffer;
    public ConnectionType threadType;
    public ClientUI cUI;
    public List<byte[]> data;
    public Protocol receiveProtocol;
    public String fileName;
    public String directory;
    public Synchronizer sync;
    public UDPManager udpm;
    public TCPManager tcpm;
    public int numBlocks;
    public int numPackets;
    public int fileSize;
    public ServerUI sUI;

    public ReceiveThread(TCPManager tcp, ConnectionType ct, Protocol p)
    {
        tcpm = tcp;
        threadType = ct;
        receiveProtocol = p;
    }

    // Constructor for Server ReceiveThread
    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        List<byte[]> d, byte[][] cp, String fn, int fs, int nb, int np, BoundedBuffer bb, ServerUI u)
    {
        data = d;
        combinedPackets = cp;
        udpm = udp;
        threadType = ct;
        receiveProtocol = p;
        buffer = b;
        fileName = fn;
        fileSize = fs;
        numBlocks = nb;
        numPackets = np;
        boundedBuffer = bb;
        sUI = u;
    }

    // Constructor for Client ReceiveThread
    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        List<byte[]> d, byte[][] cp, String fn, int fs, int nb, int np, BoundedBuffer bb, 
        String dir, Synchronizer s, ClientUI u)
    {
        data = d;
        combinedPackets = cp;
        udpm = udp;
        threadType = ct;
        receiveProtocol = p;
        buffer = b;
        fileName = fn;
        sync = s;
        fileSize = fs;
        numBlocks = nb;
        numPackets = np;
        boundedBuffer = bb;
        directory = dir;
        cUI = u;
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
        System.out.printf("RECEIVE THREAD FOR %s\n", fileName);
        FileData fd = new FileData();
        
        byte[] packet = udpm.receivePacket(buffer, 75);
        
        int identifier = packet[1];
        int scale = packet[0];

        // Remove 2 byte identifier from byte array.
        packet = fd.stripIdentifier(packet);
        
        int newSize = 0;

        if(data.size() == numBlocks - 1)
        {
            if(fileSize % buffer.length > 0 && identifier == numPackets - 1)
            {
                packet = fd.stripPadding(packet, (fileSize - ((numBlocks - 1) * 
                    ((1024 * 1024 * 4) % (buffer.length - 2)))) % (buffer.length - 2));
            }    
        }

        else
        {
            if(fileSize % buffer.length > 0 && identifier == numPackets - 1)
            {
                packet = fd.stripPadding(packet, (1024 * 1024 * 4) % (buffer.length - 2));
            }
        }

        boundedBuffer.deposit(packet);

        if(threadType == ConnectionType.Client)
        {
            FileWriter writer = new FileWriter(data, combinedPackets, buffer, fileName, fileSize, 
                identifier, scale, numBlocks, numPackets, boundedBuffer, directory, sync, cUI);

            writer.start();
        }

        else
        {
            DBWriter writer = new DBWriter(data, combinedPackets, buffer, fileName, fileSize, identifier,
                scale, numBlocks, numPackets, boundedBuffer, sUI);

            writer.start();
        }
    }
}
