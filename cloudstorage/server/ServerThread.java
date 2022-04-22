package cloudstorage.server;

import cloudstorage.data.FileData;
import cloudstorage.network.*;
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

        String[] actions = null;

        String action = tcpm.receiveMessageFromClient(2000);

        actions = action.split(" ");

        System.out.println("System " + String.valueOf(ID));

        while(true)
        {
            switch(actions[0])
            {
                case "upload":
                    uploadFile();
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

            action = tcpm.receiveMessageFromClient(2000);

            Arrays.fill(buffer, (byte)0);
        }
    }

    synchronized public void deleteFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient(2000);

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                tcpm.sendMessageToClient("File does not exist in server.", 2000);
            }

            else if(fileDeleted == 1)
            {
                tcpm.sendMessageToClient("File deleted successfully!", 2000);
            }

            else
            {
                tcpm.sendMessageToClient("Error occurred. File not deleted.", 2000);
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
            String fileName = tcpm.receiveMessageFromClient(2000);

            DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer);

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            if(files.get(fileName) != null)
            {
                tcpm.sendMessageToClient(String.valueOf(files.get(fileName).fileSize), 2000);

                udpm.sendPacketToClient(files.get(fileName).data, 
                    receivedMessage.getAddress(), receivedMessage.getPort(), 2000);
            }

            else
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 2000);

                tcpm.sendMessageToClient("File does not exist in server.", 2000);
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
        return;
    }

    synchronized public void uploadFile()
    {
        List<byte[]> packets = new ArrayList<byte[]>();
        byte[] dataBuffer = null;

        try
        {
            String fileName = tcpm.receiveMessageFromClient(2000);

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(2000));

            int fileSize = 0;

            for(int i = 0; i < numPackets; i++)
            {
                int packetSize = Integer.valueOf(tcpm.receiveMessageFromClient(2000));

                fileSize += packetSize;

                dataBuffer = new byte[packetSize];

                // Receive block from client.
                DatagramPacket receivedMessage = udpm.receivePacketFromClient(dataBuffer);

                packets.add(receivedMessage.getData());
            }

            byte[] fileData = new byte[fileSize];

            int startPosition = 0;

            // Loop through each block. Add blocks to fileData.
            for(int i = 0; i < packets.size(); i++)
            {
                for(int j = startPosition; j < fileSize - startPosition; j++)
                {
                    fileData[j] = packets.get(i)[j - startPosition];
                }

                // New start position is based on the end of the last blocks. 
                startPosition += packets.get(i).length;
            }

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            int resultCode = 0;
            
            if(files.get(fileName) == null)
            {
                resultCode = manager.insertData(fileData, fileSize);

                tcpm.sendMessageToClient(String.valueOf(resultCode), 2000);

                String message = (resultCode == 1) ? "File uploaded successfully!" : "Error occurred. File not uploaded.";

                tcpm.sendMessageToClient(message, 2000);
            }

            else
            {
                tcpm.sendMessageToClient(String.valueOf(resultCode), 2000);

                tcpm.sendMessageToClient("File already exists.", 2000);
                
                int clientResponse = Integer.valueOf(tcpm.receiveMessageFromClient(2000));

                if(clientResponse == 1)
                {
                    manager.updateFileByName(fileName, fileData);

                    tcpm.sendMessageToClient("File uploaded successfully!", 2000);
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
