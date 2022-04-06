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
/*                System.out.println("Client");
                DatagramPacket request = new DatagramPacket(new byte[1], 1, address, port);
                socket.send(request);*/

                //byte[] textToSend = "Hello World!".getBytes("UTF-8");

                System.out.println("Enter file name or Q to quite: ");
                fileName = sc.next();

                if(fileName.toLowerCase().equals("q"))
                {  
                    System.out.println("Client closed.");
                    break;
                }

                // Get file to transfer.
                try
                {
                    File targetFile = new File(fileName);

                    byte[] sendFileName = fileName.getBytes("UTF-8");

                    DatagramPacket sfn = new DatagramPacket(sendFileName, sendFileName.length, address, port);

                    socket.send(sfn);

                    // Convert file to byte array.
                    byte[] sendData = Files.readAllBytes(targetFile.toPath());

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

            while(rs.next())
            {
                int id = rs.getInt("ID");
                String fn = rs.getString("FileName");
                Blob b = rs.getBlob("Data");


                System.out.println(id);
                System.out.println(fn);
            }

            manager.closeConnection();
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

}