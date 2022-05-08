package cloudstorage.data;

import cloudstorage.control.*;
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
    public Synchronizer sync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public boolean isSending;
    public int clientID;
    public int targetPort;
    public int targetUDPPort;
    public volatile String token;
    
    public DataController(TCPManager tcp, UDPManager udp, InetAddress a, int p, InetAddress udpa, int udpp, 
        BoundedBuffer bb, ServerUI u, int ID, SQLManager sql, Synchronizer s)
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
        sync = s;
        token = "";
    }

    /*
     * \brief getBytes
     * 
     * Retrieves the value currently assigned to byteData.
     * 
     * Returns the byte[] value of byteData
    */
    public byte[] getBytes()
    {
        return byteData;
    }

    /*
     * \brief setBytes
     * 
     * Assigns a value to the object's byteData variable.
     * 
     * \param fd is the new byte[] value being assigned to byteData.
    */
    public void setBytes(byte[] fd)
    {
        byteData = fd;
    }

    /*
     * \brief setTCPManager
     * 
     * Assigns a value to the object's tcpm variable.
     * 
     * \param tcp is the new TCPManager value being assigned to tcpm.
    */
    public void setTCPManager(TCPManager tcp)
    {
        tcpm = tcp;
    }

    /*
     * \brief setUDPAddress
     * 
     * Assigns a value to the object's targetUDPAddress variable.
     * 
     * \param addr is the new InetAddress value being assigned to targetUDPAddress.
    */
    public void setUDPAddress(InetAddress addr)
    {
        targetUDPAddress = addr;
    }

    /*
     * \brief setUDPPort
     * 
     * Assigns a value to the object's targetUDPPort variable.
     * 
     * \param p is the new int value being assigned to targetUDPPort.
    */
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
        sync.setIsSending(true);
        StringBuilder sb = new StringBuilder();

        byte[] buffer = new byte[65507];

        // Split file into blocks
        fileData.createSegments(fileData.getData(), 1024 * 1024 * 4, Segment.Block);

        List<byte[]> blocksCreated = fileData.getBlocks();

        sb.append(String.format("download/%s/%d/%d", fileData.getFileName(), 
        fileData.getFileSize(), blocksCreated.size()));

        try
        {
            Thread.sleep(5000);
        }

        catch(Exception e)
        {
            
        }
        
        for(int i = 0; i < blocksCreated.size(); i++)
        {
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);
            sb.append("/" + String.valueOf(fileData.getPackets().size()));           
        }

        tcpm.sendMessageToServer(sb.toString(), 1000);

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

        sync.setIsSending(false);

        ui.appendToLog(String.format("Data transmission for %s complete.", fileData.getFileName()));
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
        ui.appendToLog(String.format("%s deleted. Updating Client %d...", fileData.getFileName(), clientID));

        tcpm.sendMessageToClient(String.format("delete/%s", fileData.getFileName()), 1000);

        ui.appendToLog("Complete.");
    }

    synchronized public void upload(FileData fileData, boolean fileIsModified, List<Integer> changedIndices)
    {
        int fileSize = fileData.getFileSize();

        try
        {
            FileData existingFile = sm.selectFileByName(fileData.getFileName());

            if(fileIsModified)
            {
                // In the case that the file is modified, the data sent over is not the complete data
                // I am using a new variable to make things a little more clear
                byte[] deltaData = fileData.getData();

                // Segment the existing data into blocks
                existingFile.createSegments(existingFile.getData(), 1024 * 1024 * 4, Segment.Block);

                List<byte[]> existingBlocks = existingFile.getBlocks();

                // Segment the delta data into blocks
                fileData.createSegments(deltaData, 1024 * 1024 * 4, Segment.Block);

                List<byte[]> deltaBlocks = fileData.getBlocks();

                for(int i = 0; i < changedIndices.size(); i++)
                {
                    // If the changed index is negative, remove it as this denotes a deleted block.
                    // Otherwise, update the block with the delta block.
                    if(changedIndices.get(i) < 0)
                    {
                        existingBlocks.remove(changedIndices.get(i));
                    }

                    else
                    {
                        existingBlocks.set(changedIndices.get(i), deltaBlocks.get(i));
                    }
                }

                byte[] newData = existingFile.combineBlockData(existingBlocks, existingBlocks.size());

                fileSize = newData.length;

                sm.updateFileByName(fileData.getFileName(), newData, fileSize);
            }

            else
            {
                if(existingFile != null)
                {
                    sm.updateFileByName(fileData.getFileName(), fileData.getData(), fileData.getFileSize());
                }

                else
                {
                    sm.insertData(fileData.getFileName(), fileData.getFileSize(), fileData.getData());
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        if(fileIsModified)
        {
            ui.appendToLog(String.format("%s has been updated. New file size is %d bytes", fileData.getFileName(), fileSize));
        }

        else
        {
            ui.appendToLog(String.format("%s of size %d bytes have been uploaded successfully.", fileData.getFileName(),
                fileData.getFileSize()));
        }

        boundedBuffer.setFileUploading(false);

        ui.appendToLog("Transmission Complete.");
    }
}
