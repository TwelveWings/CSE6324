package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.nio.file.Files;

public class FileWriter extends Thread
{
    public BoundedBuffer boundedBuffer;
    public byte[][] combinedPackets;
    public byte[] buffer;
    public String directory;
    public String fileName;
    public Synchronizer sync;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numPackets;

    public FileWriter(byte[][] cp, byte[] b, String fn, int fs, int i, int s, int np, BoundedBuffer bb, String dir, Synchronizer syn)
    {
        combinedPackets = cp;
        buffer = b;
        bufferSize = b.length;
        fileName = fn;
        fileSize = fs;
        identifier = i;
        scale = s;
        sync = syn;
        numPackets = np;
        boundedBuffer = bb;
        directory = dir;
    }

    public void run()
    {
        boolean complete = true;

        System.out.printf("ID: %d\n", identifier);
        System.out.printf("SCALE: %d\n", scale);
        System.out.printf("NUM_PACKETS: %d\n", numPackets);

        byte[] packet = boundedBuffer.withdraw();

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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try
            {
                for(int i = 0; i < numPackets; i++)
                {
                    sync.checkIfPaused();
                    bos.write(combinedPackets[i]);
                }
            }

            catch(Exception e)
            {
                e.printStackTrace();
            }

            byte[] completeData = bos.toByteArray();

            downloadFile(completeData);
        }
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
    }    
}
