import java.net.*;
import java.sql.*;
import java.util.*;

public class Server
{
    public DatagramSocket socket;
    public byte[] buffer;
    public int port;
    public Scanner sc;
    public String fileName;


    public Server(int p)
    {
        port = p;
    }

    public void startServer()
    {
        try
        {
            // Establish connection with port
            socket = new DatagramSocket(port);

            while(true)
            {
                System.out.println("Server waiting on client...");

                // 4 MB buffer to receive data
                buffer = new byte[1024 * 1024 * 4];

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
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void deleteFile()
    {
        return;
    }

    public void downloadFile()
    {
            buffer = new byte[1024 * 1024 * 4];

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

                    sendPacketToClient(fileData, receivedMessage.getAddress(), receivedMessage.getPort(), 2500);
                }

                if(count == 0)
                {
                    byte[] message = "File does not exist in server.".getBytes("UTF-8");
                    sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 2500);
                }
            }

            catch(Exception e)
            {
                e.printStackTrace();
            }

            manager.closeConnection();
    }

    public void editFile()
    {
        return;
    }

    public DatagramPacket receivePacketFromClient(byte[] buffer)
    {
        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            // Receive file name from client program.
            socket.receive(receivedPacket);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public void sendPacketToClient(byte[] data, InetAddress clientAddress, int clientPort, int timeout)
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

    public void uploadFile()
    {
        try
        {
            // 4 MB buffer to receive data
            buffer = new byte[1024 * 1024 * 4];

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromClient(buffer);

            String fileName = new String(buffer, 0, receivedMessage.getLength());

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            buffer = new byte[1024 * 1024 * 4];     
            
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
            sendPacketToClient(message, receivedMessage.getAddress(), receivedMessage.getPort(), 2500);  
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}