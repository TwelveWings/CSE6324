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
    public Date date = new Date(System.currentTimeMillis());
    public List<byte[]> data;
    public String fileName;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numBlocks;
    public int numPackets;
    public ServerUI ui;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public SQLManager sm;
    public String timestamp = formatter.format(date);

    public DBWriter(List<byte[]> d, byte[][] cp, byte[] b, String fn, int fs, int id, int s, int nb, 
        int np, BoundedBuffer bb, ServerUI u)
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
    }

    public void run()
    {
        sm = new SQLManager();
        FileData fd = new FileData();

        ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS : " + String.valueOf(identifier + 1) +
            " OUT OF " + String.valueOf(numPackets) + "\n");
        ui.textfield1.append(" [" + timestamp + "] NUM_BLOCKS: " + String.valueOf(data.size() + 1) + 
            " OUT OF " + String.valueOf(numBlocks) + "\n");

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
            byte[] blockData = fd.combineBlockData(data, numBlocks);

            // read from DB to get file currently saved.
            // compare blockData with data in DB
            // update DB data with block data
            // upload updated DB data

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

        ui.textfield1.append(" [" + timestamp + "] " + fileName + " of size " + fileSize + 
            " bytes have been uploaded succesfully \n");
        boundedBuffer.setFileUploading(false);
        ui.textfield1.append(" [" + timestamp + "] Transmission Complete \n");
    }
}