package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileWriter extends Thread
{
    public BoundedBuffer boundedBuffer;
    public byte[][] combinedPackets;
    public byte[] buffer;
    public ClientUI ui;
    public FileController controller;
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

    public FileWriter(List<byte[]> d, byte[][] cp, byte[] b, String fn, int fs, int i, int s, int nb,
        int np, BoundedBuffer bb, String dir, Synchronizer syn, ClientUI u, FileController fc)
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
        ui = u;
        controller = fc;
    }

    public void run()
    {
        boolean packetComplete = true;

        ui.appendToLog(String.format("NUM_PACKETS: %d OUT OF %d", (identifier + 1), numPackets));

        ui.appendToLog(String.format("NUM_BLOCKS: %d OUT OF %d", (fileData.size() + 1), numBlocks)); 
        
        byte[] packet = boundedBuffer.withdraw();

        combinedPackets[identifier + (128 * scale) + scale] = packet;

        
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

            FileData fd = new FileData(blockData, fileName, fileSize);

            controller.download(fd, directory);
        }
    }

    /*
     * \brief combinePacketData
     * 
     * Combines all packet data after the packets have been received. Had to be added separately from
     * the methods in FileData to account for pause/resume.
     * 
     * \param data is a jagged array used to collect the packets into a single data structure.
     * \param iterations is the number of packets that have been put into data.
     * 
     * Returns the data combined into a single byte[]
    */
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

  /*
     * \brief combinePacketData
     * 
     * Combines all block data after all the packets in each block have been received.  Had to be added
     * separately from the methods in FileData to account for pause/resume.
     * 
     * \param data is a List<byte[]> used to collect the blocks into a data structure.
     * \param iterations is the number of blocks that have been put into data.
     * 
     * Returns the data combined into a single byte[]
    */
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
}
