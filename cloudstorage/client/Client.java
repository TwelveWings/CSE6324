package cloudstorage.client;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.awt.event.*;
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
    public static TCPManager tcpm;
    public static UDPManager udpm;
    public static HashMap<String, FileData> unmodifiedFilesInDirectory;

    public static void main(String[] args)
    {
        // Instantiate the Bounded Buffer and Synchronization objects
        BoundedBuffer bb = new BoundedBuffer(1, false, false);
        Synchronizer sync = new Synchronizer();
        Synchronizer downloadSync = new Synchronizer();
        Synchronizer uploadSync = new Synchronizer();
        Splitter splitter = new Splitter();

        // Instantiate the UI.
        ClientUI ui = new ClientUI(sync);

        buffer = new byte[bufferSize];

        System.out.println("Opening Client GUI...");

        String directory = ui.selectDirectory();

        ui.appendToLog(String.format("Client will synchronize with  %s", directory));

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

            ui.appendToLog("Establishing UDP connection with server. Please wait...");

            // Send empty packet to establish UDP port connection with server.
            udpm.sendEmptyPacket(1, address, 2023);

            int connected = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            while(connected == -1)
            {
                connected = Integer.valueOf(tcpm.receiveMessageFromServer(1000));
            }

            ui.appendToLog("Client connected with Server");

            /* 
            TO DO:
            // Receive a message for the server indicating the number of files stored there
            int filesSent = Integer.valueOf(tcpm.receiveMessageFromServer(1000));

            // If there are files, send the files to the client.
            if(filesSent > 0)
            {
                System.out.println("Synchronizing with server, please wait...");
                
                for(int i = 0; i < filesSent; i++)
                {
                    String action = tcpm.receiveMessageFromServer(1000);

                    String[] components = action.split("/");

                    BoundedBuffer boundedBuffer = new BoundedBuffer(1, false, false);

                    FileController fc = new FileController(tcpm, udpm, sync, boundedBuffer, address, 2023, ui);
   
                    ClientController cc = new ClientController(tcpm, udpm, boundedBuffer, ui, components,
                        sync, fc, buffer, address, directory);

                    ClientReceiver cr = new ClientReceiver(tcpm, udpm, address, directory, sync, 
                        downloadSync, components, ui, cc);

                    cr.start();
    
                    try
                    {
                        cr.join();
                    }
    
                    catch(Exception e)
                    {
    
                    }
                }
            }*/

            Path clientDirectory = Paths.get(directory);

            unmodifiedFilesInDirectory = new HashMap<String, FileData>();

            //Get all files in directory
            List<File> filesInFolder = Files.walk(clientDirectory)
                                            .filter(Files::isRegularFile)
                                            .map(Path::toFile)
                                            .collect(Collectors.toList());

            //Load files into original files HashMap
            for (File file : filesInFolder)
            {
                String name = file.getName();

                byte[] sendData = Files.readAllBytes(file.toPath());

                FileData tempFileData = new FileData(sendData, name, sendData.length);

                tempFileData.createSegments(sendData, 1024 * 1024 * 4, Segment.Block);
                
                unmodifiedFilesInDirectory.put(name, tempFileData);
            }

            // Start event watcher to keep track of directory changes and synchronize with server.
            EventWatcher ew = new EventWatcher(tcpm, udpm, address, directory, bb, sync, downloadSync,
                uploadSync, ui, unmodifiedFilesInDirectory, splitter);

            ew.start();

            System.out.println("Client running...");

            ReceiveThread rt = new ReceiveThread(tcpm, ConnectionType.Client, Protocol.TCP, splitter);
            rt.start();

            // This is for the data synchronization from the server. Once the client receives a message
            // from the server it creates a ClientReceiver thread to handle the action.
            while(true)
            {
                while(splitter.getCommand().equals("") || splitter.getCommand().equals("wait") || splitter.getCommand().equals("ready"))
                {
                    Thread.sleep(150);
                }

                String message = splitter.getCommand();

                String[] components = message.split("/");

                BoundedBuffer boundedBuffer = new BoundedBuffer(1, false, false);

                FileController fc = new FileController(tcpm, udpm, sync, boundedBuffer, address, 2023, ui,
                    unmodifiedFilesInDirectory);

                ClientController cc = new ClientController(tcpm, udpm, boundedBuffer, ui, components,
                    sync, fc, buffer, address, directory);

                ClientReceiver cr = new ClientReceiver(tcpm, udpm, address, directory, sync, 
                    downloadSync, components, ui, cc);

                cr.start();

                try
                {
                    cr.join();
                }

                catch(Exception e)
                {

                }

                splitter.setCommand("");
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}