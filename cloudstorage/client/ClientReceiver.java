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
    public BoundedBuffer boundedBuffer;
    public byte[] buffer;
    public InetAddress address;
    public String directory;
    public Synchronizer sync;
    public Synchronizer watcherSync;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientReceiver(TCPManager tcp, UDPManager udp, InetAddress addr, byte[] b, BoundedBuffer bb, String dir, Synchronizer s, Synchronizer ws)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        buffer = b;
        boundedBuffer = bb;
        directory = dir;
        sync = s;
        watcherSync = ws;
        tcpm = tcp;
        udpm = udp;
    }

    public void run()
    {
        while(true)
        {
            String action = tcpm.receiveMessageFromServer(1000);

            System.out.println(action);

            String fileName = tcpm.receiveMessageFromServer(1000);

            if(action.equals("download"))
            {
                watcherSync.setStopWatcher(true);
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
                        ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP, buffer, data, packets,
                            fileName, fileSize, numBlocks, numPackets, boundedBuffer, directory, sync);

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

                watcherSync.resumeThread(true);
                watcherSync.setStopWatcher(false);
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
        }
    }
}
