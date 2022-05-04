package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class FileReader extends Thread
{
    public volatile byte[] data;
    public BoundedBuffer boundedBuffer;
    public String fileName;
    public Synchronizer sync;
    public Synchronizer uploadSync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int fileSize;
    public int targetPort;
    public InetAddress targetAddress;
    public volatile SystemAction command;

    public FileReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public FileReader(String fn, TCPManager tcp, UDPManager udp, int p, InetAddress a, String d, 
        BoundedBuffer bb, Synchronizer s, Synchronizer us)
    {
        data = getFileData(d + fn);
        fileName = fn;
        fileSize = data.length;
        command = null;
        tcpm = tcp;
        udpm = udp;
        sync = s;
        targetPort = p;
        targetAddress = a;
        boundedBuffer = bb;
        uploadSync = us;
    }

    public FileReader(String fn, SystemAction c, TCPManager tcp, UDPManager udp, int p, 
        InetAddress a, String d, BoundedBuffer bb, Synchronizer s, Synchronizer us)
    {
        data = getFileData(d + "/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = c;
        tcpm = tcp;
        udpm = udp;
        sync = s;
        targetPort = p;
        targetAddress = a;
        boundedBuffer = bb;
        uploadSync = us;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public void setFileSize(int fs)
    {
        fileSize = fs;
    }

    public void setCommand(SystemAction c)
    {
        command = c;
    }

    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);
        FileController fc = new FileController(fd, tcpm, udpm, sync, uploadSync, boundedBuffer, targetAddress,
            targetPort);

        if(command == SystemAction.Upload)
        {
            fc.upload();
        }

        else if(command == SystemAction.Delete)
        {
            fc.delete();
        }
    }  

    public byte[] getFileData(String fileName)
    {
        // Get file to transfer.
        File targetFile = new File(fileName);
        byte[] data = new byte[1];

        if(!targetFile.exists())
        {
            return data;
        }

        try
        {
            // Convert file to byte array.
            data = Files.readAllBytes(targetFile.toPath());
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return data;
    }
}
