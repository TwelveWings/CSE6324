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
    public ConnectionType threadType;
    public volatile SystemAction command;
    public volatile boolean deltaSync = false;
    public volatile List<String> files = new ArrayList<String>();

    public DBReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public DBReader(byte[] d, String fn, int fs, ConnectionType ct, TCPManager tcp, UDPManager udp, int p, InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        command = null;
        threadType = ct;
        tcpm = tcp;
        udpm = udp;
        targetPort = p;
        targetAddress = a;
    }

    public DBReader(byte[] d, String fn, int fs, ConnectionType ct, SystemAction c, TCPManager tcp, 
        UDPManager udp, int p, InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        threadType = ct;
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
    
    public void run()
    {
        SQLManager sm = new SQLManager();

        FileData fd = null;

        sm.setDBConnection(threadType);

        int[] differences = null;
        List<byte[]> currData = new ArrayList<byte[]>();

        fd = sm.selectFileByName(fileName);

        // If file name already has an associate DBReader thread, return.
        if(fd != null && files.contains(fileName))
        {
            return;
        }
    
        files.add(fileName);

        while(true)
        {
            fd = sm.selectFileByName(fileName);

            // If fd is not null, file exists. Otherwise it has been deleted.
            if(fd != null)
            {
                fd.createSegments(fd.getData(), fd.getData().length, Segment.Block);

                differences = fd.findChange(currData, fd.getBlocks());

                /*
                if(deltaSync)
                {
                    wait();
                }*/

                // If there is no data in the list of byte arrays, but the file size
                // is greater than 0 (indicating there is data), the file must be newly uploaded.
                // Thus the data needs to be synchronized with the server.
                if(currData.size() == 0 && fileSize > 0 || 
                    (currData.size() > 0 && differences[0] > -1 && differences[1] > -1))
                {
                    deltaSync = true;
                    JOptionPane.showMessageDialog(null, String.format("Delta Sync begin! Sending blocks [%d, %d]", differences[0], differences[1]));
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

                    //notifyAll();
                }
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
        
                    // Instantiate DatagramPacket object based on buffer.
                    String message = tcpm.receiveMessageFromServer(1000);
        
                    JOptionPane.showMessageDialog(null, message);
                }
        
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                deltaSync = false;
                setCommand(null);
            }
        }
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
