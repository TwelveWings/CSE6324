package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class Client
{
    public static InetAddress address;
    public static byte[] buffer;
    public static int port;
    public static final int blockSize = 1024 * 1024 * 4;
    public static final int bufferSize = 65507;
    public static Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;

    public static void main(String[] args)
    {
        BoundedBuffer bb = new BoundedBuffer(1, false);

        sc = new Scanner(System.in);

        buffer = new byte[bufferSize];

        System.out.println("Client running...");

        System.out.println("Please specify which directory you want to synchronize:");

        String directory = sc.nextLine();

        try
        {
            // Get address of local host.
            address = InetAddress.getLocalHost();
            
            // Establish TCP socket connection
            Socket tcpSocket = new Socket(address, 2023);

            // Establish UDP socket connection.
            DatagramSocket udpSocket = new DatagramSocket();

            // TCP and UDP helper objects to send and receive messages and packets.
            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            // Start event watcher to keep track of directory changes and synchronize with server.
            EventWatcher ew = new EventWatcher(tcpm, udpm, address, directory, bb);
            ew.start();

            while(true)
            {
                String receivedMessage = tcpm.receiveMessageFromServer(1000);

                System.out.println(receivedMessage);

                if(receivedMessage.equals("download"))
                {
                    String fileName = tcpm.receiveMessageFromServer(1000);
                    int fileSize = Integer.valueOf(tcpm.receiveMessageFromServer(1000));
                    int numPackets = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

                    byte[][] packets = new byte[numPackets][];

                    for(int i = 0; i < numPackets; i++)
                    {
                        ReceiveThread rt = new ReceiveThread(udpm, ConnectionType.Client, Protocol.UDP, buffer, packets,
                            fileName, fileSize, numPackets, bb);

                        rt.start();
                    }
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}