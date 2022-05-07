package cloudstorage.data;

import cloudstorage.control.BoundedBuffer;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataController 
{
    public BoundedBuffer boundedBuffer;
    public byte[] byteData;
    public InetAddress targetAddress;
    public InetAddress targetUDPAddress;
    public ServerUI ui;
    public SQLManager sm;
    public String timestamp;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int clientID;
    public int targetPort;
    public int targetUDPPort;
    public volatile String token;
    
    public DataController(TCPManager tcp, UDPManager udp, InetAddress a, int p, InetAddress udpa, int udpp, 
        BoundedBuffer bb, ServerUI u, int ID, SQLManager sql)
    {
        tcpm = tcp;
        udpm = udp;
        targetAddress = a;
        targetUDPAddress = udpa;
        targetPort = p;
        targetUDPPort = udpp;
        boundedBuffer = bb;
        ui = u;
        clientID = ID;
        sm = sql;
        token = "";
    }

    public byte[] getBytes()
    {
        return byteData;
    }

    public void setBytes(byte[] fd)
    {
        byteData = fd;
    }

    public void setTCPManager(TCPManager tcp)
    {
        tcpm = tcp;
    }

    public void setUDPAddress(InetAddress addr)
    {
        targetUDPAddress = addr;
    }

    public void setUDPPort(int p)
    {
        targetUDPPort = p;
    }

    /* 
     * \brief download
     * 
     * Sends the bytes of a file to a client. Bytes are sent in blocks, which are broken into packets
     * before transmission.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
    */
    synchronized public void download(FileData fileData)
    {
        StringBuilder sb = new StringBuilder();

        byte[] buffer = new byte[65507];

        // Split file into blocks
        fileData.createSegments(fileData.getData(), 1024 * 1024 * 4, Segment.Block);

        List<byte[]> blocksCreated = fileData.getBlocks();

        sb.append(String.format("download/%s/%d/%d", fileData.getFileName(), 
        fileData.getFileSize(), blocksCreated.size()));
        
        for(int i = 0; i < blocksCreated.size(); i++)
        {
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);
            sb.append("/" + String.valueOf(fileData.getPackets().size()));           
        }

        tcpm.sendMessageToServer(sb.toString(), 1000);

        System.out.printf("UDP PORT: %d\n", targetPort);

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            ui.appendToLog(String.format("Transmitting data to Client %d...", clientID));

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                boundedBuffer.deposit(packetsCreated.get(j));

                // Send the packet
                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Server,
                    Protocol.UDP, targetUDPPort, targetUDPAddress, boundedBuffer);

                st.start();

                ui.appendToLog(String.format("NUM_PACKETS: %d OUT OF %d", (packetsCreated.get(j)[1] + 1),
                    packetsCreated.size()));

                ui.appendToLog(String.format("NUM_BLOCKS: %d OUT OF %d", (i + 1), blocksCreated.size()));

                try
                {
                    st.join();
                }

                catch (Exception e)
                {

                }
            }
        }

        ui.appendToLog(String.format("Data transmission for %s", fileData.getFileName()));

        token = "";

        try
        {
            notify();
        }

        catch(Exception e)
        {

        }
    }

    /*
     * \brief delete
     * 
     * Sends the delete command to a client, to delete a file from the local directory.
     * 
     * \param fileData is the an instance of FileData which stores relevant information about the file.
    */
    synchronized public void delete(FileData fileData)
    {
        System.out.printf("CURR_FILE: %s\n", fileData.getFileName());
        System.out.printf("TOKEN: %s\n", token);
        while(!token.equals("") && !fileData.getFileName().equals(token))
        {
            try
            {
                System.out.printf("%s is waiting\n", fileData.getFileName());
                wait();
                Thread.sleep(2000);
            }

            catch(Exception e)
            {

            }
        }

        token = fileData.getFileName();

        ui.appendToLog(String.format("%s deleted. Updating Client %d...", fileData.getFileName(), clientID));

        tcpm.sendMessageToClient(String.format("delete/%s", fileData.getFileName()), 1000);

        ui.appendToLog("Complete.");

        token = "";

        try
        {
            notify();
        }

        catch(Exception e)
        {

        }
    }

    synchronized public void upload(FileData fileData)
    {
        try
        {
            FileData existingFile = sm.selectFileByName(fileData.getFileName());

            if(existingFile != null)
            {
                sm.updateFileByName(fileData.getFileName(), fileData.getData(), fileData.getFileSize());
            }

            else
            {
                sm.insertData(fileData.getFileName(), fileData.getFileSize(), fileData.getData());
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        ui.appendToLog(String.format("%s of size %d bytes have been uploaded successfully.", fileData.getFileName(),
            fileData.getFileSize()));

        boundedBuffer.setFileUploading(false);

        ui.appendToLog("Transmission Complete.");
    }
}
