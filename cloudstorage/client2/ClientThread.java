package cloudstorage.client2;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.JOptionPane;

public class ClientThread extends Thread
{
    public SystemAction threadAction;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public InetAddress address;
    public Socket tcpSocket;
    public String fileName;
    public SQLManager sm;
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
        sm = new SQLManager();

        sm.setDBConnection(ConnectionType.Client);

        switch(threadAction)
        {
            case Upload:
                uploadFile(fileName);
                break;
            case Delete:
                deleteFile(fileName);
                break;
        }

        sm.closeConnection();
    }

    public boolean checkIfPaused()
    {
        if(isPaused)
        {
            JOptionPane.showMessageDialog(null, "Thread Paused");
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

        return isPaused;
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
        boolean deleteFile = (0 == JOptionPane.showOptionDialog(
                null, "Are you sure you want to delete this file?", "Delete File", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null));

        if(!deleteFile)
        {
            return;
        }

        sm.deleteFile(fileName);

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

    synchronized public void downloadFromServer(String fileName)
    {
        // threadRunning = true;
        byte[][] packets = null;
        byte[] fileData = null;
        
        try
        {
            // Send file name to download.
            tcpm.sendMessageToServer(fileName, 1000);

            // Send datagram to establish connection
            udpm.sendPacketToServer("1".getBytes(), address, 2023, 1000);

            Arrays.fill(buffer, (byte)0);

            // The server sends the filesize that as been located.
            int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            // If fileSize == 0, there was no file. Print error from server. Otherwise convert data from server to a file.
            if(fileSize == 0)
            {
                String message = tcpm.receiveMessageFromServer(1000);

                JOptionPane.showMessageDialog(null, message);
            }

            else
            {
                // Receive a TCP message indicating the number of UDP packets being sent.
                int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                packets = new byte[numPackets][];

                FileData fd = new FileData();
                
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                // Loop through the packets that have been sent.
                for(int i = 0; i < numPackets; i++)
                {
                    String pausedMessage = (isPaused) ? "paused" : "";
                    
                    // Send a message to the server to indicate that the thread has been paused.
                    tcpm.sendMessageToServer(pausedMessage, 1000);

                    // If the thread has been paused, a wait operation will be executed in the checkIfPaused method. Once it resumes,
                    // it will return false, meaning it is no longer paused. A new message will be sent to the server to continue process.
                    if(!checkIfPaused())
                    {
                        tcpm.sendMessageToServer("resume", 1000);                        
                    }

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
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void uploadFile(String fileName)
    {
        // threadRunning = true;
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

            sm.setFileName(fileName);

            sm.insertData(sendData, sendData.length);

            FileData fd = new FileData(sendData, fileName, sendData.length);

            // Segment data byte array into packets of size <= bufferSize.
            fd.createSegments(sendData, bufferSize - 2, Segment.Packet);

            List<byte[]> packets = fd.getPackets();

            // Send server the file name of file being sent.
            tcpm.sendMessageToServer(fileName, 1000);
            
            // Send server the file size of the file being sent.
            tcpm.sendMessageToServer(String.valueOf(sendData.length), 1000);

            // Send server a message with the number of packets being sent.
            tcpm.sendMessageToServer(String.valueOf(packets.size()), 1000);

            for(int i = 0; i < packets.size(); i++)
            {
                checkIfPaused();
                // Send block data to server via UDP
                udpm.sendPacketToServer(packets.get(i), address, 2023, 1000);
            }

            int resultCode = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            String message = tcpm.receiveMessageFromServer(1000);

            JOptionPane.showMessageDialog(null, message);

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
