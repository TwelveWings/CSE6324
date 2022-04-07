import java.io.*;
import java.nio.file.Files;
import java.net.*;
import java.util.*;

public class Client
{
    public DatagramSocket socket;
    public byte[] buffer;
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
                System.out.println("What action do you want to perform? (1 - Upload, 2 - Download, 3 - Edit, 4 - Delete, 0 - Quit)");

                try 
                {
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
                        case 4:
                            deleteFile();
                            break;
                        default:
                            System.out.println("Invalid action. Please try again.");
                            break;
                    }
                }

                catch(InputMismatchException ime)
                {
                    continue;
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

            buffer = new byte[1024 * 1024 * 4];

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromServer(buffer);

            String message = new String(buffer, 0, receivedMessage.getLength());

            System.out.println(message);
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

    public DatagramPacket receivePacketFromServer(byte[] buffer)
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

    public void sendPacketToServer(byte[] data, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

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

            sendPacketToServer(sendFileName, 2500);

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());
            byte[] sendSize = String.valueOf(sendData.length).getBytes();

            sendPacketToServer(sendSize, 2500);
            sendPacketToServer(sendData, 2500);
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}