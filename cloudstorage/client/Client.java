package cloudstorage.client;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class Client
{
    public static byte[] buffer;
    public static InetAddress address;
    public static int port;
    public static final int blockSize = 1024 * 1024 * 4;
    public static final int bufferSize = 65507;
    public static Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);

        List<ClientThread> uploads = new ArrayList<ClientThread>();
        List<ClientThread> downloads = new ArrayList<ClientThread>();

        ConcurrentHashMap<String, DBReader> readers = new ConcurrentHashMap<String, DBReader>();
        ConcurrentHashMap<String, DBWriter> writers = new ConcurrentHashMap<String, DBWriter>();

        SQLManager sm = new SQLManager();

        sm.setDBConnection(ConnectionType.Client);

        // If user specifies new drop table.
        if(args.length > 0 && args[0].equals("new"))
        {
            sm.dropTable();
        }

        sm.createTable();

        try
        {
            // Get address of local host.
            address = InetAddress.getLocalHost();
            
            buffer = new byte[bufferSize];

            // Establish TCP socket connection
            Socket tcpSocket = new Socket(address, 2023);

            // Establish UDP socket connection.
            DatagramSocket udpSocket = new DatagramSocket();

            ClientThread ct;

            String[] actions = null;

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            int i = 0;
            int index = -1;
            SystemAction command = null;

            ConcurrentHashMap<String, FileData> files = sm.selectAllFiles();
            
            // Create a read thread for each file.
            files.forEach((k, v) -> readers.put(v.fileName, new DBReader(v.data, v.fileName, v.fileSize, ConnectionType.Client, tcpm, udpm, 2023, address)));

            files.forEach((k, v) -> readers.get(v.fileName).start());

            // Create a write thread for each file.
            files.forEach((k, v) -> writers.put(v.fileName, new DBWriter(v.data, v.fileName, v.fileSize, ConnectionType.Client, tcpm, udpm, 2023, address)));

            files.forEach((k, v) -> writers.get(v.fileName).start());

            byte[] data = null;

            while(true)
            {
                files = sm.selectAllFiles();
                                
                System.out.println("What action do you want to perform?\nType:\nupload <FILE>,\ndownload <FILE>,\nedit <FILE>,\ndelete <FILE>,\n" +
                    "pause <UPLOAD/DOWNLOAD> <INDEX>,\nresume <UPLOAD/DOWNLOAD> <INDEX>,\ncancel <UPLOAD/DOWNLOAD> <INDEX>,\nor quit\n");

                try 
                {
                    String action = sc.nextLine();

                    actions = action.split(" ");

                    //tcpm.sendMessageToServer(actions[0], 5000);

                    if(action.toLowerCase().equals("quit"))
                    {
                        tcpm.closeSocket();
                        udpm.closeSocket();
                        
                        System.out.println("Program terminated.");
                        return;
                    }

                    switch(actions[0].toLowerCase())
                    {
                        case "delete":
                            data = getFileData(actions[1]);

                            if(data == null)
                            {
                                break;
                            }

                            else if(!writers.containsKey(actions[1]))
                            {
                                writers.put(actions[1], new DBWriter(data, actions[1], data.length, ConnectionType.Client, SystemAction.Delete, tcpm, udpm, 2023, address));
                                writers.get(actions[1]).start();

                                readers.put(actions[1], new DBReader(data, actions[1], data.length, ConnectionType.Client, tcpm, udpm, 2023, address));
                                readers.get(actions[1]).start();
                            }

                            else
                            {
                                writers.get(actions[1]).setFileSize(data.length);
                                writers.get(actions[1]).setData(data);
                                writers.get(actions[1]).setCommand(SystemAction.Delete);

                                // If delete command is sent, delete from reader thread
                                readers.get(actions[1]).setCommand(SystemAction.Delete);
                            }

                            break;
                        case "download":
                            data = getFileData(actions[1]);
                            
                            if(data == null)
                            {
                                break;
                            }

                            else
                            {
                                readers.get(actions[1]).setFileSize(data.length);
                                readers.get(actions[1]).setData(data);
                                readers.get(actions[1]).setCommand(SystemAction.Download);
                            }

                            break;
                        case "upload":
                            data = getFileData(actions[1]);

                            if(data == null)
                            {
                                break;
                            }

                            else if(!writers.containsKey(actions[1]))
                            {
                                writers.put(actions[1], 
                                    new DBWriter(data, actions[1], data.length, ConnectionType.Client, SystemAction.Upload, tcpm, udpm, 2023, address));
                                writers.get(actions[1]).start();

                                readers.put(actions[1], new DBReader(data, actions[1], data.length, ConnectionType.Client, tcpm, udpm, 2023, address));
                                readers.get(actions[1]).start();
                            }

                            else
                            {
                                writers.get(actions[1]).setFileSize(data.length);
                                writers.get(actions[1]).setData(data);
                                writers.get(actions[1]).setCommand(SystemAction.Upload);
                            }

                            break;
                        case "pause":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).setIsPaused(true);
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                downloads.get(index).setIsPaused(true);
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        case "resume":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).setIsPaused(false);
                                uploads.get(index).resumeThread();
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                downloads.get(index).setIsPaused(false);
                                downloads.get(index).resumeThread();
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        case "cancel":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).interrupt();
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                uploads.get(index).interrupt();
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        default:
                            System.out.println("Invalid action. Please try again.");
                            break;
                    }

                    /*
                    if(command != null)
                    {
                        ct = new ClientThread(tcpSocket, udpSocket, address,  buffer, bufferSize, ++i, actions[1], command);

                        if(command == SystemAction.Upload)
                        {
                            uploads.add(ct);
                        }

                        else if(command == SystemAction.Download)
                        {
                            downloads.add(ct);
                        }

                        ct.start();
                    }*/
                }

                catch(InputMismatchException ime)
                {
                    continue;
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        sm.closeConnection();
    }

    public static byte[] getFileData(String fileName)
    {
            // Get file to transfer.
            File targetFile = new File(fileName);
            byte[] data = null;

            if(!targetFile.exists())
            {
                JOptionPane.showMessageDialog(null, "No file exists with that name.");
                return data;
            }

            try
            {
                // Convert file to byte array.
                data = Files.readAllBytes(targetFile.toPath());
            }

            catch(Exception e)
            {
                e.printStackTrace();
            }

            return data;
    }
}