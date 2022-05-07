package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ClientController 
{
    public BoundedBuffer boundedBuffer;
    public byte[] buffer;
    public ClientUI ui;
    public FileController controller;
    public InetAddress address;
    public String[] components;
    public String directory;
    public Synchronizer sync;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientController(TCPManager tcp, UDPManager udp, BoundedBuffer bb, ClientUI u, String[] comp,
        Synchronizer s, FileController fc, byte[] b, InetAddress addr, String dir)
    {
        tcpm = tcp;
        udpm = udp;
        boundedBuffer = bb;
        ui = u;
        components = comp;
        sync = s;
        controller = fc;
        buffer = b;
        address = addr;
        directory = dir;
    }

    synchronized public void downloadFile(String fileName)
    {
        boundedBuffer.setFileDownloading(true);

        int fileSize = Integer.valueOf(components[2]);
        int numBlocks = Integer.valueOf(components[3]);

        List<byte[]> data = new ArrayList<byte[]>();

        ui.appendToLog("Receiving data from Server...");

        for(int i = 0; i < numBlocks; i++)
        {
            int numPackets = Integer.valueOf(components[4 + i]);

            System.out.printf("NUM PACKETS: %d\n", numPackets);

            byte[][] packets = new byte[numPackets][];

            for(int j = 0; j < numPackets; j++)
            {
                ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP,
                    buffer, data, packets, fileName, fileSize, numBlocks, numPackets, boundedBuffer, 
                    directory, sync, ui, controller);

                rt.start();

                try
                {
                    rt.join();
                }

                catch(Exception e)
                {

                }
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

    synchronized public void deleteFile(String directory, String fileName)
    {
        try
        {
            ui.appendToLog(String.format("Synchronization complete: %s deleted!", fileName));
            Files.deleteIfExists(Paths.get(directory + "/" + fileName));
        }

        catch(Exception e)
        {

        }
    }
}
