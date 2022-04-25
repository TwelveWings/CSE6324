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
    public Socket tcpSocket;
    public String fileName;
    public TCPManager tcpm;
    public UDPManager udpm;
    public volatile boolean isPaused;
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
        isPaused = false;
    }

    public void setIsPaused(boolean pause)
    {
        isPaused = pause;
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
                downloadFile(fileName, false);
                break;
            case Edit:
                editFile(fileName);
                break;
            case Delete:
                deleteFile(fileName);
                break;
        }
    }

    public void checkIfPaused(int position)
    {
        if(isPaused)
        {
            JOptionPane.showMessageDialog(null, String.format("Thread Paused: %d", position));
            synchronized(this)
            {
                while(isPaused)
                {
                    try
                    {
                        wait();
                    }

                    catch(InterruptedException ie)
                    {
                        JOptionPane.showMessageDialog(null, "Thread Interrupted");
                    }
                }
            }
        }
    }

    public void resumeThread()
    {
        if(!isPaused)
        {
            synchronized(this)
            {
                notify();
                JOptionPane.showMessageDialog(null, "Thread Resumed");
            }
        }
    }

    synchronized public void deleteFile(String fileName)
    {
        try
        {
            // Send file to delete.
            tcpm.sendMessageToServer(fileName, 1000);

            Arrays.fill(buffer, (byte)0);

            // Instantiate DatagramPacket object based on buffer.
            String message = tcpm.receiveMessageFromServer(1000);

            JOptionPane.showMessageDialog(null, message);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public byte[] downloadFile(String fileName, boolean isEdit)
    {
        byte[][] packets = null;
        byte[] fileData = null;

        try
        {
            checkIfPaused(1);

            // Send file name to download.
            tcpm.sendMessageToServer(fileName, 1000);

            checkIfPaused(2);

            // Send datagram to establish connection
            udpm.sendPacketToServer("1".getBytes(), address, 2023, 1000);

            checkIfPaused(3);

            Arrays.fill(buffer, (byte)0);

            // The server sends the filesize that as been located.
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            // If fileSize == 0, there was no file. Print error from server. Otherwise convert data from server to a file.
            if(fileSize == 0)
            {
                String message = tcpm.receiveMessageFromServer(1000);

                checkIfPaused(4);

                JOptionPane.showMessageDialog(null, message);
            }

            else
            {
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                checkIfPaused(5);

                packets = new byte[numPackets][];

                FileData fd = new FileData();
                
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                // Loop through the packets that have been sent.
                for(int i = 0; i < numPackets; i++)
                {
                    checkIfPaused(6);
                    // Receive block from client.
                    DatagramPacket receivedMessage = udpm.receivePacketFromServer(buffer, 1000);

                    byte[] rmBytes = receivedMessage.getData();
                    int identifier = (int)rmBytes[1];
                    int scale = (int)rmBytes[0];

                    // Remove the extra bytes added to identify the order of the packet.
                    rmBytes = fd.stripIdentifier(rmBytes);

                    // If the fileSize is not evenly divisible by the bufferSize and the identifier is the last packet sent
                    // resize the packet to remove excess bytes.
                    if(fileSize % bufferSize > 0 && identifier == numPackets - 1)
                    {
                        rmBytes = fd.stripPadding(rmBytes, fileSize % (bufferSize - 2));
                    }

                    // Remove identifier and assign it in to the packets jagged array based on the identifier
                    packets[identifier + (128 * scale) + scale] = rmBytes;
                }

                for(int i = 0; i < packets.length; i++)
                {
                    bos.write(packets[i]);
                }

                fileData = bos.toByteArray();

                checkIfPaused(7);

                if(!isEdit)
                {
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
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return fileData;
    }

    synchronized public void editFile(String fileName)
    {
        String[] fileParts = fileName.split("\\.");

        if(!fileParts[fileParts.length - 1].equals("txt"))
        {
            JOptionPane.showMessageDialog(null, "You cannot edit binary files.");
            return;
        }

        byte[] fileData = downloadFile(fileName, true);

        if(fileData == null)
        {
            return;
        }

        String newValue = JOptionPane.showInputDialog(
            String.format(
                "File reads: %s. How should this be edited?", 
                new String(fileData, 0, fileData.length)));

        byte[] newData = newValue.getBytes();
                
        FileData fd = new FileData(newData, fileName, newData.length);

        // Segment data byte array into packets of size <= bufferSize.
        fd.createSegments(newData, bufferSize, Segment.Packet);

        List<byte[]> packets = fd.getPackets();

        // Send server the file name of file being sent.
        tcpm.sendMessageToServer(fileName, 1000);

        // Send server the file size of the file being sent.
        tcpm.sendMessageToServer(String.valueOf(newData.length), 1000);

        // Send server a message with the number of packets being sent.
        tcpm.sendMessageToServer(String.valueOf(packets.size()), 1000);
        
        for(int i = 0; i < packets.size(); i++)
        {
            // Send block data to server via UDP
            udpm.sendPacketToServer(packets.get(i), address, 2023, 1000);
        }

        String message = tcpm.receiveMessageFromServer(1000);

        JOptionPane.showMessageDialog(null, message);
    }

    synchronized public void uploadFile(String fileName)
    {
        try
        {
            checkIfPaused(1);

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
            fd.createSegments(sendData, bufferSize - 2, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            checkIfPaused(2);

            // Send server the file name of file being sent.
            tcpm.sendMessageToServer(fileName, 1000);
            
            checkIfPaused(3);

            // Send server the file size of the file being sent.
            tcpm.sendMessageToServer(String.valueOf(sendData.length), 1000);

            checkIfPaused(4);

            // Send server a message with the number of packets being sent.
            tcpm.sendMessageToServer(String.valueOf(packets.size()), 1000);

            for(int i = 0; i < packets.size(); i++)
            {
                checkIfPaused(5);
                // Send block data to server via UDP
                udpm.sendPacketToServer(packets.get(i), address, 2023, 1000);
            }

            checkIfPaused(6);

            int resultCode = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            checkIfPaused(7);

            String message = tcpm.receiveMessageFromServer(1000);

            checkIfPaused(8);

            JOptionPane.showMessageDialog(null, message);

            checkIfPaused(9);

            if(resultCode == 0)
            {
                String overrideFileInDB = String.valueOf(
                    JOptionPane.showOptionDialog(
                        null, "Override File?", "Upload File", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null));
                
                if(overrideFileInDB.equals("0"))
                {
                    tcpm.sendMessageToServer(overrideFileInDB, 1000);

                    message = tcpm.receiveMessageFromServer(1000);

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
