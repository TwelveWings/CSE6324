package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.views.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBWriter extends Thread
{
    public BoundedBuffer boundedBuffer;
    public byte[][] combinedPackets;
    public byte[] buffer;
    public DataController controller;
    public List<byte[]> data;
    public List<Integer> indices;
    public String fileName;
    public ServerUI ui;
    public SQLManager sm;
    public boolean fileIsModified;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int numBlocks;
    public int numPackets;
    public int scale;

    public DBWriter(List<byte[]> d, byte[][] cp, byte[] b, String fn, int fs, int id, int s, int nb, 
        int np, BoundedBuffer bb, ServerUI u, DataController dc, List<Integer> i)
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
        ui = u;
        controller = dc;
        indices = i;
        fileIsModified = (indices.size() > 0);
    }

    public void run()
    {
        sm = new SQLManager();
        FileData fd = new FileData();

        ui.appendToLog(String.format("NUM_PACKETS: %d OUT OF %d", (identifier + 1), numPackets));

        ui.appendToLog(String.format("NUM_BLOCKS: %d OUT OF %d", (data.size() + 1), numBlocks));

        boolean packetComplete = true;

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
            System.out.println("DBWRITER");
            System.out.println(fileName);
            fd.setData(fd.combineBlockData(data, numBlocks));
            fd.setFileName(fileName);
            fd.setFileSize(fileSize);

            controller.setBytes(fd.getData());

            controller.upload(fd, fileIsModified, indices);
        }
    }
}