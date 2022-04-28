package cloudstorage.data;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class DBWriter extends Thread
{
    public byte[] data;
    public String fileName;
    public int fileSize;
    public ConnectionType threadType;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int port;
    public InetAddress address;
    public volatile SystemAction command;
    public volatile boolean deltaSync = false;
    public volatile List<String> files = new ArrayList<String>();

    public DBWriter()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        threadType = null;
        command = null;
    }

    public DBWriter(byte[] d, String fn, int fs, ConnectionType ct, TCPManager tcp, UDPManager udp, int p, InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        command = null;
        threadType = ct;
        tcpm = tcp;
        udpm = udp;
        port = port;
        address = a;
    }

    public DBWriter(byte[] d, String fn, int fs, ConnectionType ct, SystemAction c, TCPManager tcp, UDPManager udp, int p, InetAddress a)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        threadType = ct;
        command = c;
        tcpm = tcp;
        udpm = udp;
        port = port;
        address = a;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public byte[] getData()
    {
        return data;
    }

    public void setFileSize(int fs)
    {
        fileSize = fs;
    }

    public int getFileSize()
    {
        return fileSize;
    }

    public void setCommand(SystemAction c)
    {
        command = c;
    }

    public SystemAction getCommand()
    {
        return command;
    }
    
    public void run()
    {
        SQLManager sm = new SQLManager();
        sm.setDBConnection(threadType);
        FileData fd = null;

        int[] differences = null;
        List<byte[]> currData = new ArrayList<byte[]>();

        fd = sm.selectFileByName(fileName);

        // If file name already has an associate DBWriter thread, return.
        if(fd != null && files.contains(fileName))
        {
            return;
        }

        files.add(fileName);

        int count = 0;
        while(true)
        {
            while(getCommand() != null)
            {
                fd = sm.selectFileByName(fileName);

                switch(getCommand())
                {
                    case Upload:
                        uploadFile(sm);
                        setCommand(null);
                        count = 0;
                        break;
                    case Delete:
                        deleteFile(sm);
                        setCommand(null);
                        count = 0;
                        break;
                }
            }
        }
    }

    public synchronized void deleteFile(SQLManager sm)
    {
        boolean deleteFile = (0 == JOptionPane.showOptionDialog(
                null, "Are you sure you want to delete this file?", "Delete File", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null));

        if(!deleteFile)
        {
            return;
        }

        sm.deleteFile(fileName);
    }

    public synchronized void uploadFile(SQLManager sm)
    {
        try
        {
            sm.setFileName(fileName);

            FileData fileData = sm.selectFileByName(fileName);

            if(fileData != null)
            {
                boolean updateFile = (0 == JOptionPane.showOptionDialog(
                    null, "Are you sure you want to override this file?", "Override File", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null));

                if(updateFile)
                {
                    sm.updateFileByName(fileName, data, fileSize);
                }
            }

            else
            {
                sm.insertData(data, data.length);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}