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

    public FileReader(String fn, TCPManager tcp, UDPManager udp, int p, InetAddress a, String d, BoundedBuffer bb, Synchronizer s, Synchronizer us)
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

    public boolean getComplete()
    {
        return complete;
    }
    
    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);

        //System.out.printf("DATALENGTH AT READ: %d", data.length);

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

            List<byte[]> blocksCreated = fd.getBlocks();

            /*
            int x = 0;
            for(int i = 0; i < blocksCreated.size(); i++)
            {
                x += blocksCreated.get(i).length;
                System.out.println(blocksCreated.get(i).length);
            }*/

           // System.out.printf("Size in FR BLOCKS: %d\n", x);

            synchronized(this)
            {
                tcpm.sendMessageToServer("upload", 1000);
                tcpm.sendMessageToServer(fileName, 1000);
                tcpm.sendMessageToServer(String.valueOf(fileSize), 1000);
                tcpm.sendMessageToServer(String.valueOf(blocksCreated.size()), 1000);

               // System.out.printf("BLOCK #: %d\n", blocksCreated.size());
                for(int i = 0; i < blocksCreated.size(); i++)
                {
                    sync.checkIfPaused();
                    
                    // Read the block and create packets
                    fd.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

                    List<byte[]> packetsCreated = fd.getPackets();

                    tcpm.sendMessageToServer(String.valueOf(packetsCreated.size()), 1000);

                   // System.out.printf("PACKET #: %d\n", packetsCreated.size());

                    for(int j = 0; j < packetsCreated.size(); j++)
                    {
                        sync.checkIfPaused();

                        boundedBuffer.deposit(packetsCreated.get(j));

                        SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Client, Protocol.UDP, targetPort, targetAddress, boundedBuffer);
                        st.start();

                        try
                        {
                            st.join();
                        }

                        catch (Exception e)
                        {

                        }
                    }
                }

                uploadSync.blockedFiles.replace(fileName, false);
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
