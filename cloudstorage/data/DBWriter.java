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
    public String fileName;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numPackets;
    public SQLManager sm;

    public DBWriter(byte[][] cp, byte[] b, String fn, int fs, int id, int s, int np, BoundedBuffer bb)
    {
        combinedPackets = cp;
        buffer = b;
        bufferSize = b.length;
        fileName = fn;
        fileSize = fs;
        identifier = id;
        scale = s;
        numPackets = np;
        boundedBuffer = bb;
    }

    public void run()
    {
        sm = new SQLManager();

        boolean complete = true;

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
                complete = false;
                break;
            }
        }

        if(complete)
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
        boundedBuffer.setFileUploaded(true);
        System.out.println(boundedBuffer.getFileUploaded());
        System.out.println("Upload complete!");
    }
}