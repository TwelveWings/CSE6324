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
    public byte[] data;
    public DataController controller;
    public String fileName;
    public SystemAction command;
    public int fileSize;

    public DBReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
    }

    public DBReader(String fn, SystemAction c, DataController dc)
    {
        fileName = fn;
        command = c;
        controller = dc;
    }

    public DBReader(byte[] d, String fn, int fs, SystemAction c, DataController dc)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
        command = c;
        controller = dc;
    }
    
    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);
    
        if(command == SystemAction.Download)
        {
            controller.download(fd);
        }

        else if(command == SystemAction.Delete)
        {
            controller.delete(fd);
        }
    }
}
