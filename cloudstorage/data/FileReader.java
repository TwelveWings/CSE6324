package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class FileReader extends Thread
{
    public byte[] data;
    public FileController controller;
    public String fileName;
    public int fileSize;
    public volatile SystemAction command;

    public FileReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public FileReader(String fn, SystemAction c, String d, FileController fc)
    {
        data = getFileData(d + "/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = c;
        controller = fc;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public void run()
    {
        FileData fd = new FileData(data, fileName, fileSize);

        if(command == SystemAction.Upload)
        {
            controller.upload(fd);
        }

        else if(command == SystemAction.Delete)
        {
            controller.delete(fd);
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
