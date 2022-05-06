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
    public Boolean fileIsModified;
    public FileData unmodifiedFileInDirectory;

    public FileReader()
    {
        data = null;
        fileName = "";
        fileSize = 0;
        command = null;
    }

    public FileReader(String fn, SystemAction c, String d, FileController fc, Boolean fim)
    {
        data = getFileData(d + "/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = c;
        controller = fc;
        fileIsModified = fim;
    }

    public FileReader(String fn, SystemAction c, String d, FileController fc, Boolean fim, FileData ufid)
    {
        data = getFileData(d + "/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = c;
        controller = fc;
        fileIsModified = fim;
        unmodifiedFileInDirectory = ufid;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public void run()
    {
        FileData fd;

        if (fileIsModified)
        {
            unmodifiedFileInDirectory.createSegments(unmodifiedFileInDirectory.getData(), 1024 * 1024 * 4, Segment.Block);

            fd = new FileData(data, fileName, fileSize, fileIsModified, unmodifiedFileInDirectory.blocks);
        }
        else
        {
            fd = new FileData(data, fileName, fileSize, fileIsModified);
        }

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
