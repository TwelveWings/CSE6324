import java.io.*;
import java.nio.file.Files;
import java.net.*;
import java.util.*;

public class Client
{
    public static DatagramSocket socket;
    public static byte[] buffer;
    public static InetAddress address;
    public static int port;
    public static Scanner sc;

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);
        try
        {
            // Get address of local host.
            address = InetAddress.getLocalHost();
            port = 17;

            buffer = new byte[1024 * 1024 * 4];

            // Establish socket connection.
            socket = new DatagramSocket();

            boolean endProgram = false;

            while(true)
            {
                System.out.println("What action do you want to perform? (1 - Upload, 2 - Download, 3 - Edit, 4 - Delete, 0 - Quit)");

                try 
                {
                    int action = sc.nextInt();

                    if(action == 0)
                    {
                        break;
                    }

                    byte[] sendAction = String.valueOf(action).getBytes("UTF-8");

                    sendPacketToServer(sendAction, 5000);
    
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
                        default:
                            System.out.println("Invalid action. Please try again.");
                            break;
                    }

                    // Clear buffer at the end of each operation.
                    Arrays.fill(buffer, (byte)0);
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

    public static void deleteFile()
    {
        String fileName = "";
        System.out.println("Enter file name or 0 to cancel:");
        fileName = sc.next();

        if(fileName.equals("0") || fileName.trim().equals(""))
        {
            System.out.println("Delete request cancelled.");
            return;
        }

        try
        {
            // Get file to transfer.
            byte[] sendFileName = fileName.getBytes("UTF-8");

            sendPacketToServer(sendFileName, 5000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromServer(buffer);

            String message = new String(buffer, 0, receivedMessage.getLength());

            System.out.println(message);
        }

        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public static void downloadFile()
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

            sendPacketToServer(sendFileName, 5000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromServer(buffer);

            int fileSize = Integer.valueOf(new String(buffer, 0, receivedMessage.getLength()));

            byte[] dataBuffer = new byte[fileSize];

            receivedMessage = receivePacketFromServer(dataBuffer);
            
            try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/" + fileName))
            {
                fos.write(dataBuffer);
                System.out.println("Download successful!");
            }

            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public static void editFile()
    {
        return;
    }

    public static DatagramPacket receivePacketFromServer(byte[] buffer)
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

    public static void sendPacketToServer(byte[] data, int timeout)
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

    public static void uploadFile()
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

            sendPacketToServer(sendFileName, 5000);

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());
            byte[] sendSize = String.valueOf(sendData.length).getBytes();

            sendPacketToServer(sendSize, 5000);
            sendPacketToServer(sendData, 5000);

            // Instantiate DatagramPacket object based on buffer.
            DatagramPacket receivedMessage = receivePacketFromServer(buffer);

            int resultCode = Integer.valueOf(new String(buffer, 0, receivedMessage.getLength()));

            receivedMessage = receivePacketFromServer(buffer);

            String message = new String(buffer, 0, receivedMessage.getLength());

            System.out.println(message);

            if(resultCode == 0)
            {
                int overrideFileInDB = 0;

                System.out.println("Override File? 1 - Yes 2 - No");
                overrideFileInDB = sc.nextInt();
        
                if(overrideFileInDB == 1)
                {
                    byte[] sendAction = String.valueOf(overrideFileInDB).getBytes("UTF-8");

                    sendPacketToServer(sendAction, 5000);

                    receivedMessage = receivePacketFromServer(buffer);

                    message = new String(buffer, 0, receivedMessage.getLength());

                    System.out.println(message);
                }

                else
                {
                    System.out.println("Upload cancelled.");
                }
            }
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}