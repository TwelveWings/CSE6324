package cloudstorage.client;

import cloudstorage.data.FileData;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import javax.swing.JOptionPane;

public class ClientThread extends Thread
{
    public Action threadAction;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public InetAddress address;
    public Scanner sc;
    public Socket tcpSocket;
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int bufferSize;
    public int ID;

    public ClientThread(Socket socketTCP, DatagramSocket socketUDP, InetAddress addr, byte[] b, int bs, int tID, String fn, Action a)
    {
        udpSocket = socketUDP;
        tcpSocket = socketTCP;
        address = addr;
        buffer = b;
        bufferSize = bs;
        ID = tID;
        fileName = fn;
        threadAction = a;
        sc = new Scanner(System.in);
    }

    public void run()
    {
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);

        switch(threadAction)
        {
            case Upload:
                uploadFile(fileName);
                break;
            case Download:
                downloadFile(fileName);
                break;
            case Edit:
                editFile(fileName);
                break;
            case Delete:
                deleteFile(fileName);
                break;
        }
    }

    public void deleteFile(String fileName)
    {
        try
        {
            // Send file to delete.
            tcpm.sendMessageToServer(fileName, 2000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            String message = tcpm.receiveMessageFromServer(2000);

            JOptionPane.showMessageDialog(null, message);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void downloadFile(String fileName)
    {
        try
        {
            // Send file name to download.
            tcpm.sendMessageToServer(fileName, 2000);

            // Send datagram to establish connection
            udpm.sendPacketToServer("1".getBytes(), address, 2023, 2000);

            Arrays.fill(buffer, (byte)0);

            // The server sends the filesize that as been located.
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(2000));

            // If fileSize == 0, there was no file. Print error from server. Otherwise convert data from server to a file.
            if(fileSize == 0)
            {
                String message = tcpm.receiveMessageFromServer(2000);

                JOptionPane.showMessageDialog(null, message);
            }

            else
            {
                byte[] dataBuffer = new byte[fileSize];

                DatagramPacket receivedMessage = udpm.receivePacketFromServer(dataBuffer);
                
                try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/" + fileName))
                {
                    fos.write(receivedMessage.getData());
                    JOptionPane.showMessageDialog(null, "Download successful!");
                }

                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void editFile(String fileName)
    {
        return;
    }

    public void uploadFile(String fileName)
    {
        try
        {
            // Get file to transfer.
            File targetFile = new File(fileName);

            if(!targetFile.exists())
            {
                JOptionPane.showMessageDialog(null, "No file exists with that name.");
            }

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());

            FileData fd = new FileData(sendData, fileName, sendData.length);

            // Segment data byte array into packets of size <= bufferSize.
            fd.createSegments(sendData, bufferSize, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            // Send server the file name of file being sent.
            tcpm.sendMessageToServer(fileName, 2000);

            // Send server a message with the number of packets being sent.
            tcpm.sendMessageToServer(String.valueOf(packets.size()), 2000);
            
            for(int i = 0; i < packets.size(); i++)
            {
                // Send size of block to server via TCP.
                tcpm.sendMessageToServer(String.valueOf(packets.get(i).length), 2000);

                // Send block data to server via UDP
                udpm.sendPacketToServer(packets.get(i), address, 2023, 2000);
            }

            int resultCode = Integer.valueOf(tcpm.receiveMessageFromServer(2000));

            String message = tcpm.receiveMessageFromServer(2000);

            JOptionPane.showMessageDialog(null, message);

            if(resultCode == 0)
            {
                String overrideFileInDB = String.valueOf(
                    JOptionPane.showOptionDialog(
                        null, "Override File?", "Upload File", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null));
                
                if(overrideFileInDB.equals("0"))
                {
                    tcpm.sendMessageToServer(overrideFileInDB, 2000);

                    message = tcpm.receiveMessageFromServer(2000);

                    JOptionPane.showMessageDialog(null, message);
                }

                else
                {
                    JOptionPane.showMessageDialog(null, "Upload cancelled.");
                }
            }
        }

        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e);
        }
    }
}
