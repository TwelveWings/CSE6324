package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ClientReceiver extends Thread
{
    public byte[] buffer;
    public InetAddress address;
    public String action;
    public String directory;
    public String fileName;
    public Synchronizer sync;
    public Synchronizer watcherSync;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientReceiver(TCPManager tcp, UDPManager udp, InetAddress addr, byte[] b, String dir, 
        Synchronizer s, Synchronizer ws, String a, String fn)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        buffer = b;
        directory = dir;
        sync = s;
        watcherSync = ws;
        tcpm = tcp;
        udpm = udp;
        action = a;
        fileName = fn;
    }

    public void run()
    {
        BoundedBuffer boundedBuffer = new BoundedBuffer(1, false);

        if(watcherSync.blockedFiles.containsKey(fileName))
        {
            watcherSync.blockedFiles.replace(fileName, true);
        }

        else
        {
            watcherSync.blockedFiles.put(fileName, true);
        }

        if(action.equals("download"))
        {
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(1000));
        
            int numBlocks = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

        // System.out.printf("NUM BLOCKS IN CR: %d\n", numBlocks);

            // Send empty packet to establish UDP port connection with server.
            udpm.sendEmptyPacket(1, address, 2023);

            List<byte[]> data = new ArrayList<byte[]>();

            for(int i = 0; i < numBlocks; i++)
            {
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                byte[][] packets = new byte[numPackets][];

                for(int j = 0; j < numPackets; j++)
                {
                    ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP,
                        buffer, data, packets, fileName, fileSize, numBlocks, numPackets, boundedBuffer, 
                        directory, sync);

                    rt.start();
                }
            }

            while(boundedBuffer.getFileUploaded())
            {
                try
                {
                    System.out.println("Waiting for file to download...");
                    Thread.sleep(1000);
                }

                catch(Exception e)
                {
                    
                }
            }
        }

        else if(action.equals("delete"))
        {
            try
            {
                Files.deleteIfExists(Paths.get(directory + "/" + fileName));
                System.out.printf("Synchronization complete: %s deleted!\n", fileName);
            }

            catch(Exception e)
            {

            }
        }

        try
        {
            Thread.sleep(3000);
        }

        catch(Exception e)
        {
            
        }

        watcherSync.blockedFiles.replace(fileName, false);
    }
}
