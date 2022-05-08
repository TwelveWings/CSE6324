package cloudstorage.server;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerController 
{
    public BoundedBuffer boundedBuffer;
    public byte[] buffer;
    public DataController controller;
    public ServerUI ui;
    public String[] components;
    public String fileName;
    public SQLManager sm;
    public TCPManager tcpm;
    public UDPManager udpm;
    public boolean fileIsModified;
    public int clientID;
    public int deltaSyncStartIndex;
    public int deltaSyncEndIndex;
    public int fileSize;
    public int numBlocks;

    // Constructor for initial client startup
    public ServerController(TCPManager tcp, UDPManager udp, SQLManager sql, ServerUI u, BoundedBuffer bb,
        DataController dc, byte[] b, int ID)
    {
        tcpm = tcp;
        udpm = udp;
        sm = sql;
        ui = u;
        boundedBuffer = bb;
        controller = dc;
        buffer = b;
        clientID = ID;
    }

    // Constructor for deleting a file
    public ServerController(TCPManager tcp, SQLManager sql, ServerUI u, String[] comp, BoundedBuffer bb,
        DataController dc, byte[] b, int ID)
    {
        tcpm = tcp;
        sm = sql;
        ui = u;
        fileName = comp[1];
        boundedBuffer = bb;
        controller = dc;
        buffer = b;
        clientID = ID;
        components = comp;
    }

    // Constructor for normal application processes
    public ServerController(TCPManager tcp, UDPManager udp, SQLManager sql, ServerUI u, String[] comp,
        BoundedBuffer bb, DataController dc, byte[] b, int ID)
    {
        tcpm = tcp;
        udpm = udp;
        sm = sql;
        ui = u;
        fileName = comp[1];
        fileSize = Integer.valueOf(comp[2]);
        numBlocks = Integer.valueOf(comp[3]);
        boundedBuffer = bb;
        controller = dc;
        buffer = b;
        clientID = ID;
        components = comp;
        fileIsModified = comp[4].equals("m");
    }

    synchronized public void deleteFile(String fileName)
    {
        try
        {
            sm.deleteFile(fileName);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void uploadFile(String fileName)
    {
        int packetStart = determinePacketStart();
        List<Integer> indices = getChangedIndices();

        try
        {
            List<byte[]> data = new ArrayList<byte[]>();

            ui.appendToLog(String.format("Receiving data from Client %d", clientID));

            for(int i = 0; i < numBlocks; i++)
            {
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(components[packetStart + i]);

                byte[][] packets = new byte[numPackets][];

                for(int j = 0; j < numPackets; j++)
                {
                    ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Server, Protocol.UDP,
                        buffer, data, packets, fileName, fileSize, numBlocks, numPackets, boundedBuffer,
                        ui, controller, indices);

                    rt.start();
                    rt.join();
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void synchronizeWithClients(String fileName, String action, SQLManager sm, ClientData client,
        BoundedBuffer bb, ServerUI ui, DataController dc)
    {        
        ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

        SystemAction command = (action.equals("delete")) ? SystemAction.Delete : SystemAction.Download;

        try
        {
            if(files.get(fileName) != null)
            {
                DBReader dbr = new DBReader(files.get(fileName).data, fileName, files.get(fileName).fileSize,
                    command, dc);

                dbr.start();
                dbr.join();
            }

            else if(command == SystemAction.Delete)
            {
                DBReader dbr = new DBReader(fileName, command, dc);

                dbr.start();
                dbr.join();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public int determinePacketStart()
    {
        // 5 -> 6
        int index = 5;
        if(fileIsModified)
        {
            for(int i = 5; i < components.length; i++)
            {
                if(components[i].equals("em"))
                {
                    index = i + 1;
                    break;
                }
            }
        }

        return index;
    }

    public List<Integer> getChangedIndices()
    {
        List<Integer> indices = new ArrayList<Integer>();

        if(!fileIsModified)
        {
            return indices;
        }

        for(int i = 5; i < components.length; i++)
        {
            if(components[i].equals("em"))
            {
                break;
            }

            indices.add(Integer.valueOf(components[i]));
        }

        return indices;
    }
}
