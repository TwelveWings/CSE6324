package cloudstorage.client;

import cloudstorage.control.*;
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
    public static TCPManager tcpm;
    public static UDPManager udpm;

    public static void main(String[] args)
    {
        BoundedBuffer bb = new BoundedBuffer(1, false);
        Synchronizer sync = new Synchronizer();
        Synchronizer downloadSync = new Synchronizer();
        Synchronizer uploadSync = new Synchronizer();

        buffer = new byte[bufferSize];

        Scanner sc = new Scanner(System.in);

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
            EventWatcher ew = new EventWatcher(tcpm, udpm, address, directory, bb, sync, downloadSync, uploadSync);
            ew.start();

            System.out.println("Client running...");

            System.out.println("Enter P or R to pause/resume any synchronization.");

            Pauser p = new Pauser(sync);
            p.start();

            while(true)
            {
                String action = tcpm.receiveMessageFromServer(1000);
                String fileName = tcpm.receiveMessageFromServer(1000);

                ClientReceiver cr = new ClientReceiver(tcpm, udpm, address, buffer, directory, sync, downloadSync, action, fileName);
                cr.start();

                try
                {
                    cr.join();
                }

                catch(Exception e)
                {

                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}