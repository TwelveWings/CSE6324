import java.net.*;
import java.sql.*;
import java.util.*;

public class Server
{
    public static DatagramSocket socket;
    public static byte[] buffer;
    public static int port;
    public static Scanner sc;

    public static void main(String[] args)
    {
        port = 17;

        // 4 MB buffer to receive data
        buffer = new byte[1024 * 1024 * 4];

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
            // Establish connection with port
            socket = new DatagramSocket(port);

            while(true)
            {
                System.out.println("Server waiting on client...");

                DatagramPacket receivedMessage = receivePacketFromClient(buffer);

                int action = Integer.valueOf(new String(buffer, 0, receivedMessage.getLength()));

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
            DatagramPacket receivedMessage = receivePacketFromClient(buffer);

            String fileName = new String(buffer, 0, receivedMessage.getLength());

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                byte[] message = "File does not exist in server.".getBytes("UTF-8");
                sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
            }

            else if(fileDeleted == -1)
            {
                byte[] message = "Error occurred. File not deleted.".getBytes("UTF-8");
                sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);                
            }

            else
            {
                byte[] message = "File deleted successfully!".getBytes("UTF-8");
                sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);  
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
                // Instantiate DatagramPacket object based on buffer.
                DatagramPacket receivedMessage = receivePacketFromClient(buffer);

                String fileName = new String(buffer, 0, receivedMessage.getLength());

                ResultSet rs = manager.selectFileByName(fileName);

                int count = 0;
                while(rs.next())
                {
                    count++;

                    byte[] fileData = rs.getBytes("Data");

                    sendPacketToClient(fileData, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
                }

                if(count == 0)
                {
                    byte[] message = "File does not exist in server.".getBytes("UTF-8");
                    sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
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

    public static DatagramPacket receivePacketFromClient(byte[] buffer)
    {
        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            // Receive file name from client program.
            socket.receive(receivedPacket);

            Thread.sleep(5000);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public static void sendPacketToClient(byte[] data, InetAddress clientAddress, int clientPort, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);

            socket.send(packet);

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
            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromClient(buffer);

            String fileName = new String(buffer, 0, receivedMessage.getLength());

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            Arrays.fill(buffer, (byte)0);
            
            // Instantiate DatagramPacket object based on buffer.
            receivedMessage = receivePacketFromClient(buffer);

            int fileSize = Integer.valueOf(new String(buffer, 0, receivedMessage.getLength()));

            buffer = new byte[fileSize];

            // Instantiate DatagramPacket object based on buffer.
            receivedMessage = receivePacketFromClient(buffer);

            // Insert file into database
            manager.insertData(buffer);

            // Close connection to DB
            manager.closeConnection();

            byte[] message = "File uploaded successfully!".getBytes("UTF-8");
            sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 5000);  
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}