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
    public byte[] data;
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int fileSize;
    public int targetPort;
    public InetAddress targetAddress;
    public SystemAction command;

    public DBReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
    }

    public DBReader(String fn, TCPManager tcp, UDPManager udp, int p, InetAddress a, SystemAction c, BoundedBuffer bb)
    {
        fileName = fn;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
        command = c;
        boundedBuffer = bb;
    }

    public DBReader(byte[] d, String fn, int fs, TCPManager tcp, UDPManager udp, int p, InetAddress a,
        SystemAction c, BoundedBuffer bb)
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
    
    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);

        byte[] buffer = new byte[65507];
    
        if(command == SystemAction.Download)
        {
            // Split file into blocks
            fd.createSegments(data, 1024 * 1024 * 4, Segment.Block);

            List<byte[]> blocksCreated = fd.getBlocks();

            tcpm.sendMessageToClient("download", 1000);
            tcpm.sendMessageToClient(fileName, 1000);
            tcpm.sendMessageToClient(String.valueOf(fileSize), 1000);
            tcpm.sendMessageToServer(String.valueOf(blocksCreated.size()), 1000);

            DatagramPacket connector = udpm.receiveDatagramPacket(buffer, 1000);

            //System.out.printf("BLOCK #: %d\n", blocksCreated.size());
            for(int i = 0; i < blocksCreated.size(); i++)
            {
                // Read the block and create packets
                fd.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

                List<byte[]> packetsCreated = fd.getPackets();

                tcpm.sendMessageToClient(String.valueOf(packetsCreated.size()), 1000);

                targetPort = connector.getPort();
                targetAddress = connector.getAddress();

                for(int j = 0; j < packetsCreated.size(); j++)
                {
                    boundedBuffer.deposit(packetsCreated.get(j));

                    // Send the packet
                    SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Server,
                        Protocol.UDP, targetPort, targetAddress, boundedBuffer);

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

        else if(command == SystemAction.Delete)
        {
            tcpm.sendMessageToClient("delete", 1000);

            try
            {
                // Send file name to delete on server.
                tcpm.sendMessageToClient(fileName, 1000);
            }
    
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
