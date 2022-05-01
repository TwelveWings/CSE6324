package cloudstorage.data;

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
    public ServerUI ui;
    public SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
    public Date date = new Date(System.currentTimeMillis());
    public String timestamp = formatter.format(date);

    public DBWriter(byte[][] cp, byte[] p, byte[] b, String fn, int fs, int i, int s, int np, ServerUI u)
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
        ui = u;
    }

    public void run()
    {
        sm = new SQLManager();

        boolean complete = true;

        FileData fd = new FileData();

        ui.textfield1.append(" [" + timestamp + "] ID: " + identifier + "\n");
        ui.textfield1.append(" [" + timestamp + "] SCALE: " + scale + "\n");
        ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS: " + numPackets + "\n");

        System.out.printf("ID: %d\n", identifier);
        System.out.printf("SCALE: %d\n", scale);
        System.out.printf("NUM_PACKETS: %d\n", numPackets);

        combinedPackets[identifier + (128 * scale) + scale] = packet;

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
            ui.textfield1.append(" [" + timestamp + "] Transmission Complete \n");
            System.out.println("Complete");
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
        ui.textfield1.append(" [" + timestamp + "] " + fileName + " of size " + fileSize + "bytes has been uploaded succesfully \n");
        System.out.println("Upload complete!");
    }
}