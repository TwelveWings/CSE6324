package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBWriter extends Thread
{
    public BoundedBuffer boundedBuffer;
    public byte[][] combinedPackets;
    public byte[] buffer;
    public List<byte[]> data;
    public String fileName;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numBlocks;
    public int numPackets;
    public SQLManager sm;

    public DBWriter(List<byte[]> d, byte[][] cp, byte[] b, String fn, int fs, int id, int s, int nb, 
        int np, BoundedBuffer bb)
    {
        data = d;
        combinedPackets = cp;
        buffer = b;
        bufferSize = b.length;
        fileName = fn;
        fileSize = fs;
        identifier = id;
        scale = s;
        numBlocks = nb;
        numPackets = np;
        boundedBuffer = bb;
    }

    public void run()
    {
        sm = new SQLManager();
        FileData fd = new FileData();

        boolean packetComplete = true;

        System.out.printf("ID: %d\n", identifier);
        System.out.printf("SCALE: %d\n", scale);
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
            byte[] packetData = fd.combinePacketData(combinedPackets, numPackets);

            data.add(packetData);
        }

        if(data.size() == numBlocks)
        {
            byte[] blockData = fd.combineBlockData(data, numBlocks);

            uploadFile(blockData);
        }
    }

    public synchronized void uploadFile(byte[] completeData)
    {
        try
        {
            FileData fileData = sm.selectFileByName(fileName);

            if(fileData != null)
            {
                sm.updateFileByName(fileName, completeData, fileSize);
            }

            else
            {
                sm.insertData(fileName, fileSize, completeData);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
        boundedBuffer.setFileUploaded(true);
        System.out.println(boundedBuffer.getFileUploaded());
        System.out.println("Upload complete!");
    }
}