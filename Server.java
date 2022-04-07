import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server
{
    public DatagramSocket socket;
    public InetAddress address;
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
                byte[] buffer = new byte[1024 * 1024 * 4];

                // Instantiate DatagramPacket object based on buffer.
                DatagramPacket receiveAction = new DatagramPacket(buffer, buffer.length);

                // Receive file name from client program.
                socket.receive(receiveAction);

                int action = Integer.valueOf(new String(buffer, 0, receiveAction.getLength()));

                switch(action)
                {
                    case 1:
                        uploadFile();
                        break;
                    case 2:
                        downloadFile();
                        break;
                    case 3:
                        // Edit a File
                        break;
                }
            }
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
            byte[] buffer = new byte[1024 * 1024 * 4];

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receiveFileName = new DatagramPacket(buffer, buffer.length);

            // Receive file name from client program.
            socket.receive(receiveFileName);

            String fileName = new String(buffer, 0, receiveFileName.getLength());

            SQLManager manager = new SQLManager(fileName);

            manager.setDBConnection();

            buffer = new byte[1024 * 1024 * 4];     
            
            DatagramPacket receiveSize = new DatagramPacket(buffer, buffer.length);

            socket.receive(receiveSize);

            int fileSize = Integer.valueOf(new String(buffer, 0, receiveSize.getLength()));

            buffer = new byte[fileSize];

            DatagramPacket receiveData = new DatagramPacket(buffer, buffer.length);

            // Receive file data
            socket.receive(receiveData);

            // Insert file into database
            manager.insertData(buffer);

            manager.closeConnection();

            System.out.println(new String(buffer, 0, receiveData.getLength()));
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void downloadFile()
    {
            byte[] buffer = new byte[1024 * 1024 * 4];

            SQLManager manager = new SQLManager();

            manager.setDBConnection();

            try
            {
                // Instantiate DatagramPacket object based on buffer.
                DatagramPacket receiveFileName = new DatagramPacket(buffer, buffer.length);

                // Receive file name from client program.
                socket.receive(receiveFileName);

                String fileName = new String(buffer, 0, receiveFileName.getLength());

                ResultSet rs = manager.selectFileByName(fileName);

                while(rs.next())
                {
                    int id = rs.getInt("ID");
                    String fn = rs.getString("FileName");
                    byte[] fileData = rs.getBytes("Data");

                    System.out.println(id);
                    System.out.println(fn);
                    System.out.println(new String(fileData, 0, fileData.length));
                }
            }

            catch(Exception e)
            {
                e.printStackTrace();
            }

            manager.closeConnection();
    }
}