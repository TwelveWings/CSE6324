import java.net.*;
import java.sql.*;
import java.util.*;

public class ServerThread extends Thread
{
    public static Action action;
    public static TCPManager tcpm;
    public static UDPManager udpm;
    public int ID;
    public Scanner sc;
    public static byte[] buffer;
    public static final int blockSize = 1024 * 1024 * 4;
    public static int bufferSize;

    public ServerThread(TCPManager tcpManager, UDPManager udpManager, byte[] b, int bs, int tID)
    {
        tcpm = tcpManager;
        udpm = udpManager;
        ID = tID;
        buffer = b;
        bufferSize = bs;
    }

    public ServerThread(TCPManager tcpManager, UDPManager udpManager, byte[] b, int bs, int tID, Action a)
    {
        tcpm = tcpManager;
        udpm = udpManager;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        action = a;
    }

    public void run()
    {
        while(true)
        {
            String receivedMessage = tcpm.receiveMessageFromClient();

            int action = (receivedMessage != null) ? Integer.valueOf(receivedMessage) : 0;

            System.out.println("System " + String.valueOf(ID));

            switch(action)
            {
                case 1:
                    uploadFile();
                    break;
                case 2:
                    downloadFile();
                    break;
                case 3:
                    editFile();
                    break;
                case 4:
                    deleteFile();
                    break;
            }
            Arrays.fill(buffer, (byte)0);
        }
    }

    public static void deleteFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                tcpm.sendMessageToClient("File does not exist in server.", 5000);
            }

            else if(fileDeleted == 1)
            {
                tcpm.sendMessageToClient("File deleted successfully!", 5000);
            }

            else
            {
                tcpm.sendMessageToClient("Error occurred. File not deleted.", 5000);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    public static void downloadFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer);

            ResultSet rs = manager.selectFileByName(fileName);

            int count = 0;
            while(rs.next())
            {
                count++;

                byte[] fileData = rs.getBytes("Data");

                String fileSize = String.valueOf(fileData.length);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 5000);

                // Send data using UDP
                udpm.sendPacketToClient(fileData, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
            }

            if(count == 0)
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 5000);

                tcpm.sendMessageToClient("File does not exist in server.", 5000);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    public static void editFile()
    {
        return;
    }

    public static void uploadFile()
    {
        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            Arrays.fill(buffer, (byte)0);

            List<byte[]> packets = new ArrayList<byte[]>();
            byte[] dataBuffer = null;

            int numBlocks = Integer.valueOf(tcpm.receiveMessageFromClient());
            int fileSize = 0;

            for(int i = 0; i < numBlocks; i++)
            {
                int blockSize = Integer.valueOf(tcpm.receiveMessageFromClient());

                fileSize += blockSize;

                dataBuffer = new byte[blockSize];

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

            // Insert file into database
            int fileAdded = manager.insertData(fileData, fileSize);
            int clientResponse = 0;

            String resultCode = String.valueOf(fileAdded);

            tcpm.sendMessageToClient(resultCode, 5000);

            if(fileAdded == 0)
            {
                tcpm.sendMessageToClient("File already exists.", 5000);
                
                clientResponse = Integer.valueOf(tcpm.receiveMessageFromClient());

                if(clientResponse == 1)
                {
                    manager.updateFileByName(fileName, dataBuffer);

                    tcpm.sendMessageToClient("File uploaded successfully!", 5000);
                }
            }

            else if(fileAdded == 1)
            {
                tcpm.sendMessageToClient("File uploaded successfully!", 5000);
            }

            else
            {
                tcpm.sendMessageToClient("Error occurred. File not uploaded.", 5000);
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
