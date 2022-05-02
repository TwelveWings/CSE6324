package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.FileData;
import cloudstorage.enums.Segment;
import cloudstorage.network.*;

import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
    public static HashMap<String, FileData> originalFilesInDirectory;

    public static void main(String[] args)
    {
        BoundedBuffer bb = new BoundedBuffer(1, false);
        Synchronizer sync = new Synchronizer();

        sc = new Scanner(System.in);

        buffer = new byte[bufferSize];

        System.out.println("Please specify which directory you want to synchronize:");

        String directory = sc.nextLine();

        try
        {
            // Local directory converted to a Path.
            Path clientDirectory = Paths.get(directory);

            originalFilesInDirectory= new HashMap<String, FileData>();

            //Get all files in directory
            List<File> filesInFolder = Files.walk(clientDirectory)
                                            .filter(Files::isRegularFile)
                                            .map(Path::toFile)
                                            .collect(Collectors.toList());

            //Load files into original files HashMap
            for (File file : filesInFolder)
            {
                String fileName = file.getName();

                byte[] sendData = Files.readAllBytes(file.toPath());

                FileData tempFileData = new FileData(sendData, fileName, sendData.length);

                tempFileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);
                
                originalFilesInDirectory.put(fileName, tempFileData);
            }

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
            EventWatcher ew = new EventWatcher(tcpm, udpm, address, directory, bb, sync, originalFilesInDirectory);
            ew.start();

            ClientReceiver cr = new ClientReceiver(tcpm, udpm, address, buffer, bb, directory, sync);
            cr.start();

            System.out.println("Client running...");

            System.out.println("Enter P or R to pause/resume any synchronization.");

            while(true)
            {
                String command = sc.nextLine();

                switch(command.toLowerCase())
                {
                    case "p":
                        sync.setIsPaused(true);
                        break;
                    case "r":
                        sync.setIsPaused(false);
                        sync.resumeThread();
                        break;
                    default:
                        System.out.println("Invalid action.");
                        break;
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}