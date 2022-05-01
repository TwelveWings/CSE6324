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
    public DatagramPacket packet;
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

        if(threadType == ConnectionType.Client)
        {
            packet = udpm.receivePacketFromServer(buffer, 1000);

            byte[] rp = packet.getData();

            System.out.printf("RP: %d\n", rp[1]);

            int identifier = (int)rp[1];
            int scale = (int)rp[0];

            rp = fd.stripIdentifier(rp);

            if(fileSize % buffer.length > 0 && identifier == numPackets - 1)
            {
                rp = fd.stripPadding(rp, fileSize % (buffer.length - 2));
            }

            if(rp == null)
            {
                System.out.println("Cry");
            }

            boundedBuffer.deposit(rp);

            FileWriter writer = new FileWriter(combinedPackets, buffer, fileName, fileSize, identifier, scale, numPackets, boundedBuffer, directory, sync);
            writer.start();
        }

        else
        {
            // Weird bug occurred where identifier was being incremented by 1 when constructing
            // the writer object. This section of the code has been moved here and has fixed the issue.
            packet = udpm.receivePacketFromClient(buffer, 1000);

            System.out.println(packet.getPort());

            byte[] rp = packet.getData();

            //System.out.printf("RP: %d\n", rp[1]);

            int identifier = (int)rp[1];
            int scale = (int)rp[0];

            rp = fd.stripIdentifier(rp);

            if(fileSize % buffer.length > 0 && identifier == numPackets - 1)
            {
                rp = fd.stripPadding(rp, fileSize % (buffer.length - 2));
            }

            boundedBuffer.deposit(rp);

            DBWriter writer = new DBWriter(combinedPackets, buffer, fileName, fileSize, identifier, scale, numPackets, boundedBuffer);
            writer.start();
        }
    }
}
