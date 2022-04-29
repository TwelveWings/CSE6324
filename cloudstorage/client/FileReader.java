package cloudstorage.client;

import cloudstorage.data.*;
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
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int fileSize;
    public int targetPort;
    public InetAddress targetAddress;
    public boolean complete = false;
    public volatile SystemAction command;
    public volatile Set<String> files = new HashSet<String>();

    public FileReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public FileReader(String fn, TCPManager tcp, UDPManager udp, int p, InetAddress a)
    {
        data = getFileData(System.getProperty("user.dir") + "/cloudstorage/client/files/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = null;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
    }

    public FileReader(String fn, SystemAction c, TCPManager tcp, UDPManager udp, int p, 
        InetAddress a)
    {
        data = getFileData(System.getProperty("user.dir") + "/cloudstorage/client/files/" + fn);
        fileName = fn;
        fileSize = data.length;
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

        // If file name already has an associate FileReader thread, return.
        if(files.contains(fileName))
        {
            return;
        }

        files.add(fileName);
    
        if(command == SystemAction.Upload)
        {
            // Split file into blocks
            fd.createSegments(data, 1024 * 1024 * 4, Segment.Block);

            synchronized(this)
            {
                tcpm.sendMessageToServer("upload", 1000);
                tcpm.sendMessageToServer(fileName, 1000);
                tcpm.sendMessageToServer(String.valueOf(fileSize), 1000);

                for(int i = 0; i < fd.getBlocks().size(); i++)
                {
                    // Read the block and create packets
                    fd.createSegments(fd.getBlocks().get(i), 65505, Segment.Packet);

                    tcpm.sendMessageToServer(String.valueOf(fd.getPackets().size()), 1000);

                    // Send the packet
                    SendThread st = new SendThread(udpm, fd.getPackets(), ConnectionType.Client, Protocol.UDP, targetPort, targetAddress);
                    st.start();
                }
            }
        }

        else if(command == SystemAction.Delete)
        {
            tcpm.sendMessageToServer("delete", 1000);

            try
            {
                // Send file name to delete on server.
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

    public void downloadFile()
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
