import java.io.*;
import java.nio.file.Files;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.sql.rowset.serial.SerialBlob;

public class Client
{
    public int port;
    public Scanner sc;

    public Client(int p)
    {
        port = p;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int p)
    {
        port = p;
    }

    public void startClient()
    {
        sc = new Scanner(System.in);
        try
        {
            // Get address of local host.
            InetAddress address = InetAddress.getLocalHost();
            System.out.println(address);

            // Establish socket connection.
            DatagramSocket socket = new DatagramSocket();

            String fileName = "";

            while(true)
            {
                System.out.println("Enter file name or Q to quite: ");
                fileName = sc.next();

                if(fileName.toLowerCase().equals("q"))
                {  
                    System.out.println("Client closed.");
                    break;
                }

                try
                {
                    // Get file to transfer.
                    File targetFile = new File(fileName);

                    byte[] sendFileName = fileName.getBytes("UTF-8");

                    DatagramPacket sfn = new DatagramPacket(sendFileName, sendFileName.length, address, port);

                    socket.send(sfn);

                    // Wait for 5000 ms to ensure previous datagram packet has been sent.
                    Thread.sleep(5000);

                    // Convert file to byte array.
                    byte[] sendData = Files.readAllBytes(targetFile.toPath());

                    byte[] sendSize = String.valueOf(sendData.length).getBytes();

                    // Instantiate datagram packet to send.
                    DatagramPacket fileSize = new DatagramPacket(sendSize, sendSize.length, address, port);

                    socket.send(fileSize);

                    Thread.sleep(5000);

                    // Instantiate datagram packet to send.
                    DatagramPacket fileData = new DatagramPacket(sendData, sendData.length, address, port);

                    socket.send(fileData);
                }

                catch(Exception e)
                {
                    System.out.println(e);
                }
            }

            SQLManager manager = new SQLManager();

            manager.setDBConnection();

            ResultSet rs = manager.selectData();

            if(rs == null)
            {
                System.out.println("uh oh!");
            }

            else
            {
                while(rs.next())
                {
                    int id = rs.getInt("ID");
                    String fn = rs.getString("FileName");
                    byte[] buffer = rs.getBytes("Data");

                    System.out.println(id);
                    System.out.println(fn);
                    System.out.println(new String(buffer, 0, buffer.length));
                }

                manager.closeConnection();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}