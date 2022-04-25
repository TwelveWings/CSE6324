package cloudstorage.server;

import cloudstorage.enums.*;
import cloudstorage.data.FileData;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread extends Thread
{
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Socket tcpSocket;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int ID;
    public final int blockSize = 1024 * 1024 * 4;
    public int bufferSize;

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
    }

    public void run()
    {
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);

        String action = tcpm.receiveMessageFromClient(500);

        System.out.println("System " + String.valueOf(ID));

        if(action.equals("quit"))
        {
            return;
        }

        while(true)
        {
            System.out.println(action);

            switch(action)
            {
                case "upload":
                    uploadFile(false);
                    break;
                case "download":
                    downloadFile();
                    break;
                case "edit":
                    editFile();
                    break;
                case "delete":
                    deleteFile();
                    break;
            }

            action = tcpm.receiveMessageFromClient(500);

            if(action.equals("quit"))
            {
                return;
            }

            Arrays.fill(buffer, (byte)0);
        }
    }

    synchronized public void deleteFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient(500);

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                tcpm.sendMessageToClient("File does not exist in server.", 500);
            }

            else if(fileDeleted == 1)
            {
                tcpm.sendMessageToClient("File deleted successfully!", 500);
            }

            else
            {
                tcpm.sendMessageToClient("Error occurred. File not deleted.", 500);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    synchronized public void downloadFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient(500);

            // Establish UDP connection with client.
            DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer, 500);

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            if(files.get(fileName) != null)
            {
                FileData fd = new FileData(files.get(fileName).data, fileName, files.get(fileName).fileSize);
                fd.createSegments(fd.data, bufferSize - 2, Segment.Packet);

                List<byte[]> packets = fd.getPackets();

                // Send client the file size
                tcpm.sendMessageToClient(String.valueOf(files.get(fileName).fileSize), 500);

                // Send client the number of packets that will be sent.
                tcpm.sendMessageToClient(String.valueOf(packets.size()), 500);

                for(int i = 0; i < packets.size(); i++)
                {
                    // Send block data to server via UDP
                    udpm.sendPacketToClient(packets.get(i), receivedMessage.getAddress(), receivedMessage.getPort(), 500);
                }
            }

            else
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 500);

                tcpm.sendMessageToClient("File does not exist in server.", 500);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    synchronized public void editFile()
    {
        downloadFile();
        uploadFile(true);
    }

    synchronized public void uploadFile(boolean isEdit)
    {
        byte[][] packets = null;

        try
        {
            // Receive a TCP message indicating the name of the file being sent.
            String fileName = tcpm.receiveMessageFromClient(500);

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            int fileSize = Integer.valueOf(tcpm.receiveMessageFromClient(500));

            // Receive a TCP message indicating the number of UDP packets being sent.
            int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(500));

            packets = new byte[numPackets][];

            FileData fd = new FileData();
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Loop through the packets that have been sent.
            for(int i = 0; i < numPackets; i++)
            {
                // Receive block from client.
                DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer, 500);

                byte[] rmBytes = receivedMessage.getData();
                int identifier = (int)rmBytes[1];
                int scale = (int)rmBytes[0];

                // Remove the extra byte added to identify the order of the packet.
                rmBytes = fd.stripIdentifier(rmBytes);

                // If the fileSize is not evenly divisible by the bufferSize and the identifier is the last packet sent
                // resize the packet to remove excess bytes.
                if(fileSize % bufferSize > 0 && identifier == numPackets - 1)
                {
                    rmBytes = fd.stripPadding(rmBytes, fileSize % (bufferSize - 2));
                }

                // Remove identifier and assign it in to the packets jagged array based on the identifier
                packets[identifier + (128 * scale) + scale] = rmBytes;
            }

            for(int i = 0; i < packets.length; i++)
            {
                bos.write(packets[i]);
            }

            byte[] fileData = bos.toByteArray();

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            int resultCode = 0;
            
            if(files.get(fileName) == null)
            {
                resultCode = manager.insertData(fileData, fileSize);

                tcpm.sendMessageToClient(String.valueOf(resultCode), 500);

                String message = (resultCode == 1) ? "File uploaded successfully!" : "Error occurred. File not uploaded.";

                tcpm.sendMessageToClient(message, 500);
            }

            else if(isEdit)
            {
                manager.updateFileByName(fileName, fileData, fileData.length);
                tcpm.sendMessageToClient("File updated!", 500);
            }

            else
            {
                tcpm.sendMessageToClient(String.valueOf(resultCode), 500);

                tcpm.sendMessageToClient("File already exists.", 500);
                
                int clientResponse = Integer.valueOf(tcpm.receiveMessageFromClient(500));

                if(clientResponse == 1)
                {
                    manager.updateFileByName(fileName, fileData, fileData.length);

                    tcpm.sendMessageToClient("File uploaded successfully!", 500);
                }
            }

            // Close connection to DB
            manager.closeConnection();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
