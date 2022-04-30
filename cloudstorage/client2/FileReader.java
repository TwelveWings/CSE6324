package cloudstorage.client2;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class FileReader extends Thread
{
    public volatile byte[] data;
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int fileSize;
    public int targetPort;
    public InetAddress targetAddress;
    public boolean complete = false;
    public volatile SystemAction command;
    public volatile boolean deltaSync = false;
    public volatile Set<String> files = new HashSet<String>();

    public FileReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public FileReader(byte[] d, String fn, int fs, TCPManager tcp, UDPManager udp, int p, InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        command = null;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
    }

    public FileReader(byte[] d, String fn, int fs, SystemAction c, TCPManager tcp, UDPManager udp, int p, 
        InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        command = c;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
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

    public boolean getComplete()
    {
        return complete;
    }
    
    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);

        int[] differences = null;
        List<byte[]> currData = new ArrayList<byte[]>();

        // If file name already has an associate DBReader thread, return.
        if(files.contains(fileName))
        {
            return;
        }

        files.add(fileName);
    
        System.out.println(fileName);

        if(command == SystemAction.Upload)
        {
            synchronized(this)
            {
                fd.createSegments(fd.getData(), 65505, Segment.Packet);

                tcpm.sendMessageToServer("upload", 1000);
                tcpm.sendMessageToServer(fileName, 1000);
                tcpm.sendMessageToServer(String.valueOf(fileSize), 1000);
                tcpm.sendMessageToServer(String.valueOf(fd.getPackets().size()), 1000);

                for(int i = 0; i < fd.getPackets().size(); i++)
                {
                    udpm.sendPacketToServer(fd.getPackets().get(i), targetAddress, targetPort, 1000);
                }
            }
        }

        else if(command == SystemAction.Delete)
        {
            tcpm.sendMessageToServer("delete", 1000);
            try
            {
                // Send file to delete.
                tcpm.sendMessageToServer(fileName, 1000);
            }
    
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        files.remove(fileName);

        complete = true;
    }
}
