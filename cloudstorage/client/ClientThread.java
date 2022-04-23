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
    public SystemAction threadAction;
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

    public ClientThread(Socket socketTCP, DatagramSocket socketUDP, InetAddress addr, byte[] b, int bs, int tID, String fn, SystemAction a)
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

    synchronized public void deleteFile(String fileName)
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

    synchronized public void downloadFile(String fileName)
    {
        byte[][] packets = null;

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
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(2000));

                packets = new byte[numPackets][];

                FileData fd = new FileData();
                
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                // Loop through the packets that have been sent.
                for(int i = 0; i < numPackets; i++)
                {
                    // Receive block from client.
                    DatagramPacket receivedMessage = udpm.receivePacketFromServer(buffer);

                    byte[] rmBytes = receivedMessage.getData();
                    int identifier = (int)rmBytes[1];
                    int scale = (int)rmBytes[0];

                    // Remove the extra byte added to identify the order of the packet.
                    rmBytes = fd.stripIdentifier(rmBytes);

                    // If the fileSize is not evenly divisible by the bufferSize and the identifier is the last packet sent
                    // resize the packet to remove excess bytes.
                    if(fileSize % bufferSize > 0 && identifier == numPackets - 1)
                    {
                        rmBytes = fd.stripPadding(rmBytes, fileSize % bufferSize);
                    }

                    // Remove identifier and assign it in to the packets jagged array based on the identifier
                    packets[identifier + (128 * scale) + scale] = fd.stripIdentifier(rmBytes);
                }

                for(int i = 0; i < packets.length; i++)
                {
                    bos.write(packets[i]);
                }

                byte[] fileData = bos.toByteArray();

                try(FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/cloudstorage/downloads/" + fileName))
                {
                    fos.write(fileData);
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

    synchronized public void editFile(String fileName)
    {
        return;
    }

    synchronized public void uploadFile(String fileName)
    {
        try
        {
            // Get file to transfer.
            File targetFile = new File(fileName);

            if(!targetFile.exists())
            {
                JOptionPane.showMessageDialog(null, "No file exists with that name.");
                return;
            }

            // Convert file to byte array.
            byte[] sendData = Files.readAllBytes(targetFile.toPath());

            FileData fd = new FileData(sendData, fileName, sendData.length);

            // Segment data byte array into packets of size <= bufferSize.
            fd.createSegments(sendData, bufferSize, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            // Send server the file name of file being sent.
            tcpm.sendMessageToServer(fileName, 2000);

            // Send server the file size of the file being sent.
            tcpm.sendMessageToServer(String.valueOf(sendData.length), 2000);

            // Send server a message with the number of packets being sent.
            tcpm.sendMessageToServer(String.valueOf(packets.size()), 2000);
            
            for(int i = 0; i < packets.size(); i++)
            {
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
