package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import cloudstorage.server.view.*;
import java.text.SimpleDateFormat;

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
    public ServerUI ui;
    public SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
    public Date date = new Date(System.currentTimeMillis());
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
        ui = u;
        boundedBuffer = bb;
    }

    public void run()
    {
        sm = new SQLManager();
        FileData fd = new FileData();

        ui.textfield1.append(" [" + timestamp + "] ID: " + identifier + "\n");
        ui.textfield1.append(" [" + timestamp + "] SCALE: " + scale + "\n");
        ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS: " + numPackets + "\n");
        
        boolean packetComplete = true;

        System.out.printf("ID: %d\n", identifier);
        //System.out.printf("SCALE: %d\n", scale);
        System.out.printf("NUM_PACKETS: %d\n", numPackets);

        byte[] packet = boundedBuffer.withdraw();

        //System.out.printf("PACKET_LEN: %d\n", packet.length);

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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] packetData = fd.combinePacketData(combinedPackets, numPackets);

            //System.out.printf("PACKET SIZE: %d\n", packetData.length);

            data.add(packetData);
        }

        if(data.size() == numBlocks)
        {
            byte[] blockData = fd.combineBlockData(data, numBlocks);

            // read from DB to get file currently saved.
            // compare blockData with data in DB
            // update DB data with block data
            // upload updated DB data

           // System.out.printf("Data Before Upload: %d\n", blockData.length);

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
        ui.textfield1.append(" [" + timestamp + "] " + fileName + " of size " + fileSize + " bytes has been uploaded succesfully \n");
        boundedBuffer.setFileUploaded(true);
        System.out.println(boundedBuffer.getFileUploaded());
        ui.textfield1.append(" [" + timestamp + "] Transmission Complete \n");
        System.out.println("Complete");
    }
}