import java.io.*;
import java.nio.file.Files;
import java.net.*;
import java.util.*;

public class Client
{
    public DatagramSocket socket;
    public InetAddress address;
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
            address = InetAddress.getLocalHost();
            System.out.println(address);

            // Establish socket connection.
            socket = new DatagramSocket();

            boolean endProgram = false;

            while(!endProgram)
            {
                System.out.println("What action do you want to perform? (1 - Upload, 2 - Download, 3 - Edit, 0 - Quit)");
                int action = sc.nextInt();

                byte[] sendAction = String.valueOf(action).getBytes("UTF-8");

                DatagramPacket sAction = new DatagramPacket(sendAction, sendAction.length, address, port);

                socket.send(sAction);
    
                // Wait for 2500 ms to ensure previous datagram packet has been sent.
                Thread.sleep(2500);

                switch(action)
                {
                    case 0:
                        endProgram = true;
                        break;
                    case 1:
                        uploadFile();
                        break;
                    case 2:
                        downloadFile();
                        break;
                    case 3:
                        editFile();
                        break;
                    default:
                        System.out.println("Invalid action. Please try again.");
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
        String fileName = "";
        System.out.println("Enter file name or 0 to cancel:");
        fileName = sc.next();

        if(fileName.equals("0") || fileName.trim().equals(""))
        {
            System.out.println("Upload cancelled.");
            return;
        }

        try
        {
            // Get file to transfer.
            File targetFile = new File(fileName);

            byte[] sendFileName = fileName.getBytes("UTF-8");

            DatagramPacket sfn = new DatagramPacket(sendFileName, sendFileName.length, address, port);

            socket.send(sfn);

            // Wait for 2500 ms to ensure previous datagram packet has been sent.
            Thread.sleep(2500);

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());

            byte[] sendSize = String.valueOf(sendData.length).getBytes();

            // Instantiate datagram packet to send.
            DatagramPacket fileSize = new DatagramPacket(sendSize, sendSize.length, address, port);

            socket.send(fileSize);

            Thread.sleep(2500);

            // Instantiate datagram packet to send.
            DatagramPacket fileData = new DatagramPacket(sendData, sendData.length, address, port);

            socket.send(fileData);

            Thread.sleep(2500);
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public void downloadFile()
    {
        String fileName = "";
        System.out.println("Enter file name or 0 to cancel:");
        fileName = sc.next();

        if(fileName.equals("0") || fileName.trim().equals(""))
        {
            System.out.println("Download cancelled.");
            return;
        }

        try
        {
            // Get file to transfer.
            byte[] sendFileName = fileName.getBytes("UTF-8");

            DatagramPacket sfn = new DatagramPacket(sendFileName, sendFileName.length, address, port);

            socket.send(sfn);

            // Wait for 5000 ms to ensure previous datagram packet has been sent.
            Thread.sleep(5000);
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public void editFile()
    {
        return;
    }

}