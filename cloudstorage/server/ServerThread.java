package cloudstorage.server;

import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread extends Thread
{
    public BoundedBuffer bb;
    public byte[] buffer;
    public DatagramSocket udpSocket;
    public Socket tcpSocket;
    public SQLManager sm;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int ID;
    public final int blockSize = 1024 * 1024 * 4;
    public int bufferSize;
    public List<ClientData> clients;

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID, List<ClientData> cd)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        clients = cd;
    }

    public void run()
    {
        bb = new BoundedBuffer(1);
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);
        sm = new SQLManager();

        sm.setDBConnection();
        
        String action = tcpm.receiveMessageFromClient(1000);

        if(action.equals("quit"))
        {
            return;
        }

        while(true)
        {
            System.out.printf("Active Clients: %d\n", clients.size());
            System.out.printf("Thread %d peforming %s\n", ID, action);

            switch(action)
            {
                case "upload":
                    uploadFile();
                    break;
                case "download":
                    downloadFile();
                    break;
                case "delete":
                    deleteFile();
                    break;
            }

            action = tcpm.receiveMessageFromClient(1000);

            if(action.equals("quit"))
            {
                sm.closeConnection();
                return;
            }

            Arrays.fill(buffer, (byte)0);
        }
    }

    synchronized public void deleteFile()
    {
        try
        {
            String fileName = tcpm.receiveMessageFromClient(1000);

            int fileDeleted = sm.deleteFile(fileName);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void downloadFile()
    {
        try
        {
            String fileName = tcpm.receiveMessageFromClient(1000);

            // Establish UDP connection with client.
            DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer, 1000);

            ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

            if(files.get(fileName) != null)
            {
                FileData fd = new FileData(files.get(fileName).data, fileName, files.get(fileName).fileSize);
                fd.createSegments(fd.data, bufferSize - 2, Segment.Packet);

                List<byte[]> packets = fd.getPackets();

                // Send client the file size
                tcpm.sendMessageToClient(String.valueOf(files.get(fileName).fileSize), 1000);

                // Send client the number of packets that will be sent.
                tcpm.sendMessageToClient(String.valueOf(packets.size()), 1000);

                String pausedMessage = "";

                for(int i = 0; i < packets.size(); i++)
                {
                    // Receive TCP message from client indicating if the process is paused.
                    pausedMessage = tcpm.receiveMessageFromClient(1000);

                    // If the process is paused, wait until a new message is received to indicate that the process should continue.
                    pausedMessage = tcpm.receiveMessageFromClient(1000);

                    // Send block data to server via UDP
                    udpm.sendPacketToClient(packets.get(i), receivedMessage.getAddress(), receivedMessage.getPort(), 1000);
                }
            }

            else
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 1000);

                tcpm.sendMessageToClient("File does not exist in server.", 1000);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void uploadFile()
    {
        try
        {
            // Receive a TCP message indicating the name of the file being sent.
            String fileName = tcpm.receiveMessageFromClient(1000);

            int fileSize = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

            // Receive a TCP message indicating the number of UDP packets being sent.
            int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient(1000));

            //packets = new byte[numPackets][];

            //FileData fd = new FileData();
            

            for(int i = 0; i < numPackets; i++)
            {
                ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Server, Protocol.UDP, buffer,
                    fileName, fileSize, numPackets);
                rt.start();
            }

            /*
            // Loop through the packets that have been sent.
            for(int i = 0; i < numPackets; i++)
            {
                // Receive block from client.
                DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer, 1000);

                byte[] rmBytes = receivedMessage.getData();
                int identifier = (int)rmBytes[1];
                int scale = (int)rmBytes[0];

                // If the identifier is greater than or equal to the number of packets, it cannot be
                // part of the current data being received. Thus, discard it and try again.
                if(identifier >= numPackets)
                {
                    i--;
                    continue;
                }

                System.out.printf("TID: Thread %d\n", ID);
                System.out.printf("ID: %d\n", identifier);
                System.out.printf("SCALE: %d\n", scale);
                System.out.printf("NUM_PACKETS: %d\n", numPackets);

                // If an empty datapacket is sent, leave the method as the thread has been interrupted.
                if(Arrays.equals(rmBytes, empty))
                {
                    // Receive last packet and do nothing with it.
                    udpm.receivePacketFromClient(buffer, 1000);

                    System.out.println("Upload Cancelled");

                    return;
                }

                // Remove the extra byte added to identify the order of the packet.
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

            byte[] fileData = bos.toByteArray();

            ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();

            int resultCode = 0;
            
            if(files.get(fileName) == null)
            {
                resultCode = sm.insertData(fileData, fileSize);
            }

            else
            {
                System.out.println(fileName);
                System.out.println(fileData.length);
                sm.updateFileByName(fileName, fileData, fileData.length);
            }

            System.out.println("Upload complete!");*/
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
