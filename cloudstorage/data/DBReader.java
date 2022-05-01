package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.network.*;
import cloudstorage.enums.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBReader extends Thread
{
    public BoundedBuffer boundedBuffer;
    public volatile byte[] data;
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int fileSize;
    public int targetPort;
    public InetAddress targetAddress;
    public boolean complete = false;
    public SystemAction command;
    public volatile Set<String> files = new HashSet<String>();

    public DBReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
    }

    public DBReader(byte[] d, String fn, int fs, TCPManager tcp, UDPManager udp, int p, InetAddress a, SystemAction c, BoundedBuffer bb)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
        command = c;
        boundedBuffer = bb;
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

        byte[] buffer = new byte[65507];
    
        if(command == SystemAction.Download)
        {
            // Split file into blocks
            fd.createSegments(data, 1024 * 1024 * 4, Segment.Block);

            synchronized(this)
            {
                tcpm.sendMessageToClient("download", 1000);
                tcpm.sendMessageToClient(fileName, 1000);
                tcpm.sendMessageToClient(String.valueOf(fileSize), 1000);
                tcpm.sendMessageToServer(String.valueOf(fd.getBlocks().size()), 1000);

                for(int i = 0; i < fd.getBlocks().size(); i++)
                {
                    // Read the block and create packets
                    fd.createSegments(fd.getBlocks().get(i), 65505, Segment.Packet);

                    tcpm.sendMessageToClient(String.valueOf(fd.getPackets().size()), 1000);

                    DatagramPacket connector = udpm.receivePacketFromClient(buffer, 1000);

                    targetPort = connector.getPort();
                    targetAddress = connector.getAddress();

                    for(int j = 0; j < fd.getPackets().size(); j++)
                    {
                        boundedBuffer.deposit(fd.getPackets().get(j));

                        // Send the packet
                        SendThread st = new SendThread(udpm, fd.getPackets(), ConnectionType.Server, Protocol.UDP, targetPort, targetAddress, boundedBuffer);
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
}
