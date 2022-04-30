package cloudstorage.data;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.nio.file.Files;

public class FileWriter extends Thread
{
    public byte[][] combinedPackets;
    public byte[] buffer;
    public byte[] packet;
    public String fileName;
    public int bufferSize;
    public int fileSize;
    public int identifier;
    public int scale;
    public int numPackets;

    public FileWriter(byte[][] cp, byte[] p, byte[] b, String fn, int fs, int i, int s, int np)
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

    public void run()
    {
        boolean complete = true;

        FileData fd = new FileData();

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

            downloadFile(completeData);
        }
    }

    public void downloadFile(byte[] data)
    {
        try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/cloudstorage/client2/files" + fileName))
        {
            fos.write(data);
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }    
}
