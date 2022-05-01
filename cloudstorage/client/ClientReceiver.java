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
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientReceiver(TCPManager tcp, UDPManager udp, InetAddress addr, byte[] b, BoundedBuffer bb, String dir, Synchronizer s)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        buffer = b;
        directory = dir;
        sync = s;
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
                int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(1000));
            
                int numBlocks = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                // Send empty packet to establish UDP port connection with server.
                udpm.sendEmptyPacket(1, address, 2023);

                byte[][] packets = new byte[numPackets][];

                for(int i = 0; i < numBlocks; i++)
                {
                    for(int j = 0; j < numPackets; j++)
                    {
                        ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP, buffer, packets,
                            fileName, fileSize, numPackets, boundedBuffer, directory, sync);

                        rt.start();
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
        }
    }
}
