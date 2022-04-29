package cloudstorage.data;

import cloudstorage.network.*;
import cloudstorage.enums.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBReader extends Thread
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

    public DBReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public DBReader(byte[] d, String fn, int fs, TCPManager tcp, UDPManager udp, int p, InetAddress a)
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

    public DBReader(byte[] d, String fn, int fs, SystemAction c, TCPManager tcp, UDPManager udp, int p, 
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
            // Split file into blocks
            fd.createSegments(data, 1024 * 1024 * 4, Segment.Block);

            differences = fd.findChange(currData, fd.getBlocks());

            /*
            if(deltaSync)
            {
                wait();
            }*/

            synchronized(this)
            {
                deltaSync = true;
                fd.createSegments(fd.getData(), 65505, Segment.Packet);

                tcpm.sendMessageToServer("upload", 1000);
                tcpm.sendMessageToServer(fileName, 1000);
                tcpm.sendMessageToServer(String.valueOf(fileSize), 1000);
                tcpm.sendMessageToServer(String.valueOf(fd.getPackets().size()), 1000);

                for(int i = 0; i < fd.getPackets().size(); i++)
                {
                    udpm.sendPacketToServer(fd.getPackets().get(i), targetAddress, targetPort, 1000);
                }

                currData = fd.getBlocks();
                deltaSync = false;
            }

            //notifyAll();
        }

        else if(command == SystemAction.Delete)
        {
            deltaSync = true;
            JOptionPane.showMessageDialog(null, "Delta Sync begin!");
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
            deltaSync = false;
        }

        files.remove(fileName);

        complete = true;
    }

    public void downloadFile(SQLManager sm)
    {
        byte[] fileData = null;
        
        fileData = data;

        try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/cloudstorage/downloads/" + fileName))
        {
            fos.write(fileData);
            JOptionPane.showMessageDialog(null, "Download successful!");
        }

        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}
