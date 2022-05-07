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
    public HashMap<String, FileData> filesInDirectory;
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

    public FileReader(String fn, SystemAction c, String d, FileController fc, HashMap<String, FileData> fid)
    {
        data = getFileData(d + "/" + fn);
        fileName = fn;
        fileSize = data.length;
        command = c;
        controller = fc;
        filesInDirectory = fid;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public void run()
    {
        if(command == SystemAction.Upload)
        {
            FileData fd = filesInDirectory.get(fileName);

            controller.upload(fd);
        }

        else if(command == SystemAction.Delete)
        {
            controller.delete(fileName);
        }
    }  

    /*
     * \brief getFileData
     * 
     * Gets the data from a file
     * 
     * \param fileName is the name of the file being converted to a byte[]
     * 
     * Returns the byte[] of the converted file.
    */
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
