import java.io.*;
import java.net.*;
import java.util.*;
import javax.sql.rowset.serial.SerialBlob;

public class Server
{
    public String fileName;
    public int port;

    public Server(int p, String fn)
    {
        port = p;
        fileName = fn;
    }

    public void startServer()
    {
        try
        {
            // Establish connection with port
            DatagramSocket socket = new DatagramSocket(port);

            while(true)
            {
                System.out.println("Server waiting on client...");

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
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}