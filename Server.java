import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

public class Server
{
    public static DatagramSocket udpSocket;
    public static byte[] buffer;
    public static int port;
    public static final int bufferSize = 1024 * 1024 * 4;
    public static Scanner sc;
    public static ServerSocket serverSocket;
    public static Socket tcpSocket;

    public static void main(String[] args)
    {
        port = 17;

        // 4 MB buffer to receive data
        buffer = new byte[bufferSize];

        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        // If user specifies new drop table.
        if(args.length > 0 && args[0].equals("new"))
        {
            manager.dropTable();
        }

        manager.createTable();

        manager.closeConnection();

        try
        {
            // Establish TCP connection with port
            serverSocket = new ServerSocket(port);
            tcpSocket = serverSocket.accept();

            // Establish UPD connection with port
            udpSocket = new DatagramSocket(port);

            while(true)
            {
                String receivedMessage = receiveMessageFromClient();

                int action = (receivedMessage != null) ? Integer.valueOf(receivedMessage) : 0;

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

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void deleteFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = receiveMessageFromClient();

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                sendMessageToClient("File does not exist in server.", 5000);
            }

            else if(fileDeleted == 1)
            {
                sendMessageToClient("File deleted successfully!", 5000);
            }

            else
            {
                sendMessageToClient("Error occurred. File not deleted.", 5000);
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
            String fileName = receiveMessageFromClient();

            DatagramPacket receivedMessage = receivePacketFromClient(buffer);

            ResultSet rs = manager.selectFileByName(fileName);

            int count = 0;
            while(rs.next())
            {
                count++;

                byte[] fileData = rs.getBytes("Data");

                String fileSize = String.valueOf(fileData.length);

                // Send file size using TCP
                sendMessageToClient(fileSize, 5000);

                // Send data using UDP
                sendPacketToClient(fileData, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
            }

            if(count == 0)
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                sendMessageToClient(fileSize, 5000);

                sendMessageToClient("File does not exist in server.", 5000);
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

    public static String receiveMessageFromClient()
    {
        String message = "";

        try
        {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

            message = fromClient.readLine();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return message;
    }

    public static DatagramPacket receivePacketFromClient(byte[] buffer)
    {
        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            // Receive file name from client program.
            udpSocket.receive(receivedPacket);

            Thread.sleep(5000);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public static void sendMessageToClient(String message, int timeout)
    {
        try
        {
            PrintWriter toClient = new PrintWriter(tcpSocket.getOutputStream(), true);
        
            toClient.println(message);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPacketToClient(byte[] data, InetAddress clientAddress, int clientPort, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);

            udpSocket.send(packet);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void uploadFile()
    {
        try
        {
            String fileName = receiveMessageFromClient();

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            Arrays.fill(buffer, (byte)0);
            
            int fileSize = Integer.valueOf(receiveMessageFromClient());

            byte[] dataBuffer = new byte[fileSize];

            // Receive data from client using UDP.
            DatagramPacket receivedMessage = receivePacketFromClient(dataBuffer);

            // Insert file into database
            int fileAdded = manager.insertData(dataBuffer);
            int clientResponse = 0;

            String resultCode = String.valueOf(fileAdded);

            sendMessageToClient(resultCode, 5000);

            if(fileAdded == 0)
            {
                sendMessageToClient("File already exists.", 5000);
                
                clientResponse = Integer.valueOf(receiveMessageFromClient());

                if(clientResponse == 1)
                {
                    manager.updateFileByName(fileName, dataBuffer);

                    sendMessageToClient("File uploaded successfully!", 5000);
                }
            }

            else if(fileAdded == 1)
            {
                sendMessageToClient("File uploaded successfully!", 5000);
            }

            else
            {
                sendMessageToClient("Error occurred. File not uploaded.", 5000);
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