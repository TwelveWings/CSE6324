package cloudstorage.client;

import cloudstorage.data.FileData;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class ClientThread extends Thread
{
    public Action action;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public InetAddress address;
    public Scanner sc;
    public Socket tcpSocket;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int bufferSize;
    public int ID;

    public ClientThread(Socket socketTCP, DatagramSocket socketUDP, InetAddress addr, byte[] b, int bs, int tID, Action a)
    {
        udpSocket = socketUDP;
        tcpSocket = socketTCP;
        address = addr;
        buffer = b;
        bufferSize = bs;
        ID = tID;
        action = a;
        sc = new Scanner(System.in);
    }

    public void run()
    {
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);

        switch(action)
        {
            case Upload:
                uploadFile();
                break;
            case Download:
                downloadFile();
                break;
            case Edit:
                editFile();
                break;
            case Delete:
                deleteFile();
                break;
        }
    }

    public void deleteFile()
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
            tcpm.sendMessageToServer(fileName, 5000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            String message = tcpm.receiveMessageFromServer();

            System.out.println(message);
        }

        catch (Exception e)
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
            // Send file name to download.
            tcpm.sendMessageToServer(fileName, 5000);

            // Send datagram to establish connection
            udpm.sendPacketToServer("1".getBytes(), address, 2023, 5000);

            Arrays.fill(buffer, (byte)0);

            // The server sends the filesize that as been located.
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer());

            // If fileSize == 0, there was no file. Print error from server. Otherwise convert data from server to a file.
            if(fileSize == 0)
            {
                String message = tcpm.receiveMessageFromServer();

                System.out.println(message);
            }

            else
            {
                byte[] dataBuffer = new byte[fileSize];

                DatagramPacket receivedMessage = udpm.receivePacketFromServer(dataBuffer);
                
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

    public void editFile()
    {
        return;
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

            if(!targetFile.exists())
            {
                System.out.println("No file exists with that name.");
            }

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());

            FileData fd = new FileData(sendData, fileName, sendData.length);

            // Segment data byte array into packets of size <= bufferSize.
            fd.createSegments(sendData, bufferSize, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            // Send server the file name of file being sent.
            tcpm.sendMessageToServer(fileName, 5000);

            // Send server a message with the number of packets being sent.
            tcpm.sendMessageToServer(String.valueOf(packets.size()), 5000);
            
            for(int i = 0; i < packets.size(); i++)
            {
                // Send size of block to server via TCP.
                tcpm.sendMessageToServer(String.valueOf(packets.get(i).length), 5000);

                // Send block data to server via UDP
                udpm.sendPacketToServer(packets.get(i), address, 2023, 5000);
            }

            int resultCode = Integer.valueOf(tcpm.receiveMessageFromServer());

            String message = tcpm.receiveMessageFromServer();

            System.out.println(message);

            if(resultCode == 0)
            {
                System.out.println("Override File? 1 - Yes 2 - No");
                String overrideFileInDB = sc.next();
        
                if(overrideFileInDB.equals("1"))
                {
                    tcpm.sendMessageToServer(overrideFileInDB, 5000);

                    message = tcpm.receiveMessageFromServer();

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
