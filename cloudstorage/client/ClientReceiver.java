package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientReceiver extends Thread
{
    public byte[] buffer;
    public ClientUI ui;
    public Date date = new Date(System.currentTimeMillis());
    public InetAddress address;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String[] components;
    public String action;
    public String directory;
    public String fileName;
    public String timestamp = formatter.format(date);
    public Synchronizer sync;
    public Synchronizer downloadSync;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientReceiver(TCPManager tcp, UDPManager udp, InetAddress addr, byte[] b, String dir, 
        Synchronizer s, Synchronizer ds, String[] c, ClientUI u)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        buffer = b;
        directory = dir;
        sync = s;
        downloadSync = ds;
        tcpm = tcp;
        udpm = udp;
        ui = u;
        components = c;
    }

    public void run()
    {
        String action = components[0];
        String fileName = components[1];

        BoundedBuffer boundedBuffer = new BoundedBuffer(1, false, false);

        if(downloadSync.blockedFiles.containsKey(fileName))
        {
            downloadSync.blockedFiles.replace(fileName, true);
        }

        else
        {
            downloadSync.blockedFiles.put(fileName, true);
        }

        if(action.equals("download"))
        {
            boundedBuffer.setFileDownloading(true);

            int fileSize = Integer.valueOf(components[2]);
            int numBlocks = Integer.valueOf(components[3]);

            // Send empty packet to establish UDP port connection with server.
            udpm.sendEmptyPacket(1, address, 2023);

            ui.textfield1.append(" [" + timestamp + "] Receiving data from Server...\n");

            List<byte[]> data = new ArrayList<byte[]>();

            for(int i = 0; i < numBlocks; i++)
            {
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                byte[][] packets = new byte[numPackets][];

                for(int j = 0; j < numPackets; j++)
                {
                    ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP,
                        buffer, data, packets, fileName, fileSize, numBlocks, numPackets, boundedBuffer, 
                        directory, sync, ui);

                    rt.start();
                }
            }

            while(boundedBuffer.getFileDownloading())
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

        downloadSync.blockedFiles.replace(fileName, false);
    }
}
