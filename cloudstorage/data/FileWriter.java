package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileWriter extends Thread
{
    public BoundedBuffer boundedBuffer;
    public byte[][] combinedPackets;
    public byte[] buffer;
    public List<byte[]> fileData;
    public String directory;
    public String fileName;
    public Synchronizer sync;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numBlocks;
    public int numPackets;

    public FileWriter(List<byte[]> d, byte[][] cp, byte[] b, String fn, int fs, int i, int s, int nb, int np, BoundedBuffer bb, 
        String dir, Synchronizer syn)
    {
        fileData = d;
        combinedPackets = cp;
        buffer = b;
        bufferSize = b.length;
        fileName = fn;
        fileSize = fs;
        identifier = i;
        scale = s;
        sync = syn;
        numBlocks = nb;
        numPackets = np;
        boundedBuffer = bb;
        directory = dir;
    }

    public void run()
    {
        boolean packetComplete = true;

        System.out.printf("ID: %d\n", identifier);
        System.out.printf("NUM_PACKETS: %d\n", numPackets);

        byte[] packet = boundedBuffer.withdraw();

        synchronized(this)
        {
            combinedPackets[identifier + (128 * scale) + scale] = packet;
        }

        for(int i = 0; i < combinedPackets.length; i++)
        {
            if(combinedPackets[i] == null)
            {
                packetComplete = false;
                break;
            }
        }

        if(packetComplete)
        {
            byte[] packetData = combinePacketData(combinedPackets, numPackets);

            fileData.add(packetData);
        }

        if(fileData.size() == numBlocks)
        {
            byte[] blockData = combineBlockData(fileData, numBlocks);

            downloadFile(blockData);
        }
    }

    public byte[] combinePacketData(byte[][] data, int iterations)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try
        {
            for(int i = 0; i < iterations; i++)
            {
                sync.checkIfPaused();
                bos.write(data[i]);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public byte[] combineBlockData(List<byte[]> data, int iterations)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try
        {
            for(int i = 0; i < iterations; i++)
            {
                sync.checkIfPaused();
                bos.write(data.get(i));
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public void downloadFile(byte[] data)
    {
        try(FileOutputStream fos = new FileOutputStream(directory + "/" + fileName))
        {
            fos.write(data);


            System.out.printf("Synchronization complete: %s added/updated!\n", fileName);
        }
       
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }

        boundedBuffer.setFileUploaded(true);
        System.out.println(boundedBuffer.getFileUploaded());
    }    
}
