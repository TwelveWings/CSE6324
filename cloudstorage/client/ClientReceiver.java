package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.util.*;

public class ClientReceiver extends Thread
{
    public ClientController controller;
    public ClientUI ui;
    public FileController Fc;
    public InetAddress address;
    public String[] components;
    public String action;
    public String directory;
    public String fileName;
    public Synchronizer sync;
    public Synchronizer downloadSync;
    public TCPManager tcpm;
    public UDPManager udpm;

    public ClientReceiver(TCPManager tcp, UDPManager udp, InetAddress addr, String dir, Synchronizer s,
        Synchronizer ds, String[] c, ClientUI u, ClientController cc)
    {
        tcpm = tcp;
        udpm = udp;
        address = addr;
        directory = dir;
        sync = s;
        downloadSync = ds;
        tcpm = tcp;
        udpm = udp;
        ui = u;
        components = c;
        controller = cc;
    }

    public void run()
    {
        ui.appendToLog("Connecting with server...");

        String action = components[0];
        String fileName = components[1];

        if(downloadSync.blockedFiles.containsKey(fileName))
        {
            downloadSync.blockedFiles.replace(fileName, true);
        }

        else
        {
            downloadSync.blockedFiles.put(fileName, true);
        }

        switch(action)
        {
            case "download":
                controller.downloadFile(fileName);
                break;
            case "delete":
                controller.deleteFile(directory, fileName);
                break;
        }

        try
        {
            Thread.sleep(3000);
        }

        catch(Exception e)
        {
            
        }

        downloadSync.blockedFiles.replace(fileName, false);
    }
}
