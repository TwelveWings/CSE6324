package cloudstorage.data;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBWriter extends Thread
{
    public byte[][] combinedPackets;
    public byte[] packet;
    public byte[] buffer;
    public String fileName;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numPackets;
    public SQLManager sm;
    public volatile int packetsProcessed = 0;

    public DBWriter(byte[][] cp, int i, int s, byte[] p, byte[] b, String fn, int fs, int np)
    {
        combinedPackets = cp;
        buffer = b;
        bufferSize = b.length;
        packet = p;
        fileName = fn;
        fileSize = fs;
        identifier = i;
        scale = s;
        numPackets = np;
    }

    public void setPacketsProcessed(int p)
    {
        packetsProcessed = p;
    }
    
    public void run()
    {
        sm = new SQLManager();

        FileData fd = new FileData();

        System.out.printf("ID: %d\n", identifier);
        System.out.printf("SCALE: %d\n", scale);
        System.out.printf("NUM_PACKETS: %d\n", numPackets);

        combinedPackets[identifier + (128 * scale) + scale] = packet;

        packetsProcessed++;
        setPacketsProcessed(packetsProcessed);

        //System.out.println(packetsProcessed);

        if(packetsProcessed == numPackets)
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try
            {
                for(int i = 0; i < numPackets; i++)
                {
                    bos.write(combinedPackets[i]);
                }
            }

            catch(Exception e)
            {
                e.printStackTrace();
            }

            byte[] completeData = bos.toByteArray();

            uploadFile(completeData);

            packetsProcessed = 0;
        }
    }

    /*
    public synchronized void deleteFile(SQLManager sm)
    {
        boolean deleteFile = (0 == JOptionPane.showOptionDialog(
                null, "Are you sure you want to delete this file?", "Delete File", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null));

        if(!deleteFile)
        {
            return;
        }

        sm.deleteFile(fileName);
    }*/

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

        System.out.println("Upload complete!");
    }
}