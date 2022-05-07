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
    public DataController dataController;
    public FileController fileController;
    public List<byte[]> data;
    public List<Integer> indices;
    public Protocol receiveProtocol;
    public ServerUI sUI;
    public String fileName;
    public String directory;
    public Synchronizer sync;
    public UDPManager udpm;
    public TCPManager tcpm;
    public int numBlocks;
    public int numPackets;
    public int fileSize;

    public ReceiveThread(TCPManager tcp, ConnectionType ct, Protocol p)
    {
        tcpm = tcp;
        threadType = ct;
        receiveProtocol = p;
    }

    // Constructor for Server ReceiveThread
    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        List<byte[]> d, byte[][] cp, String fn, int fs, int nb, int np, BoundedBuffer bb, ServerUI u,
        DataController dc, List<Integer> i)
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
        dataController = dc;
        indices = i;
    }

    // Constructor for Client ReceiveThread
    public ReceiveThread(UDPManager udp, ConnectionType ct, Protocol p, byte[] b,
        List<byte[]> d, byte[][] cp, String fn, int fs, int nb, int np, BoundedBuffer bb, 
        String dir, Synchronizer s, ClientUI u, FileController fc)
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
        fileController = fc;
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

  /*
     * \brief receiveTCP
     * 
     * Receive TCP commands
     * 
     * \param tcpm is the TCPManager instance being used.
     * \param threadType is to determine if the request is coming from a client or server.
    */
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

  /*
     * \brief receiveUDP
     * 
     * Receive UDP packet
     * 
     * \param udpm is the UDPManager instance being used.
     * \param threadType is to determine if the request is coming from a client or server.
    */
    public synchronized void receiveUDP(UDPManager udpm, ConnectionType threadType)
    {
        FileData fd = new FileData();
        
        byte[] packet = udpm.receivePacket(buffer, 75);
        
        int identifier = packet[1];
        int scale = packet[0];

        // Remove 2 byte identifier from byte array.
        packet = fd.stripIdentifier(packet);
        
        int newSize = 0;

        // If its the last packet sent, it may not be equal to the full packet size of 65505. As such,
        // strip the padding. 
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
                identifier, scale, numBlocks, numPackets, boundedBuffer, directory, sync, cUI, fileController);

            writer.start();
        }

        else
        {
            DBWriter writer = new DBWriter(data, combinedPackets, buffer, fileName, fileSize, identifier,
                scale, numBlocks, numPackets, boundedBuffer, sUI, dataController, indices);

            writer.start();
        }
    }
}
