import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class Client
{
    public static DatagramSocket udpSocket;
    public static byte[] buffer;
    public static InetAddress address;
    public static int port;
    public static final int blockSize = 1024 * 1024 * 4;
    public static final int bufferSize = 65536;
    public static Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;


    public static void main(String[] args)
    {
        sc = new Scanner(System.in);
        try
        {
            // Get address of local host.
            address = InetAddress.getLocalHost();
            port = 17;

            buffer = new byte[bufferSize];

            // Establish TCP socket connection
            Socket tcpSocket = new Socket(address, port);

            // Establish UDP socket connection.
            udpSocket = new DatagramSocket();

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            while(true)
            {
                System.out.println("What action do you want to perform? (1 - Upload, 2 - Download, 3 - Edit, 4 - Delete, 0 - Quit)");

                try 
                {
                    String action = sc.next();

                    if(action.equals("0"))
                    {
                        break;
                    }

                    sendMessageToServer(action, 5000);
    
                    switch(Integer.valueOf(action))
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

            tcpm.closeSocket();
            udpm.closeSocket();
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
            // Send file to delete.
            sendMessageToServer(fileName, 5000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            String message = receiveMessageFromServer();

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
            // Send file name to download.
            sendMessageToServer(fileName, 5000);

            // Send datagram to establish connection
            udpm.sendPacketToServer("1".getBytes(), address, port, 5000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            int fileSize = Integer.valueOf(receiveMessageFromServer());

            if(fileSize == 0)
            {
                String message = receiveMessageFromServer();

                System.out.println(message);
            }

            else
            {
                byte[] dataBuffer = new byte[fileSize];

                DatagramPacket receivedMessage = receivePacketFromServer(dataBuffer);
                
                try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/" + fileName))
                {
                    fos.write(receivedMessage.getData());
                    System.out.println("Download successful!");
                }

                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }
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

    public static String receiveMessageFromServer()
    {
        String message = "";

        try
        {
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(tcpm.tcpSocket.getInputStream()));
            message = fromServer.readLine();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return message;
    }


    public static DatagramPacket receivePacketFromServer(byte[] buffer)
    {
        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            // Receive file name from client program.
            udpSocket.receive(receivedPacket);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public static void sendMessageToServer(String message, int timeout)
    {
        try
        {
            PrintWriter toServer = new PrintWriter(tcpm.tcpSocket.getOutputStream(), true);
            toServer.println(message);
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

            sendMessageToServer(fileName, 5000);

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());

            FileData fd = new FileData(sendData, fileName, sendData.length);

            // Segment data byte array into packets of size <= bufferSize.
            fd.createSegments(sendData, bufferSize, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            // Send server a message with the number of packets being sent.
            sendMessageToServer(String.valueOf(packets.size()), 5000);
            
            for(int i = 0; i < packets.size(); i++)
            {
                // Send size of block to server via TCP.
                sendMessageToServer(String.valueOf(packets.get(i).length), 5000);

                // Send block data to server via UDP
                udpm.sendPacketToServer(packets.get(i), address, port, 5000);
            }

            int resultCode = Integer.valueOf(receiveMessageFromServer());

            String message = receiveMessageFromServer();

            System.out.println(message);

            if(resultCode == 0)
            {
                System.out.println("Override File? 1 - Yes 2 - No");
                String overrideFileInDB = sc.next();
        
                if(overrideFileInDB.equals("1"))
                {
                    sendMessageToServer(overrideFileInDB, 5000);

                    message = receiveMessageFromServer();

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