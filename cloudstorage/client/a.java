public class a 
{
        
}

/*
package cloudstorage.client;

import cloudstorage.enums.*;
import cloudstorage.data.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

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

            String fileDirectory = System.getProperty("user.dir") + "/cloudstorage/files/";

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            ConcurrentHashMap<String, FileData> cloudFiles = new ConcurrentHashMap<String, FileData>();

            boolean listUpdated = false;
            boolean fileDeleted = true;

            while(true)
            {
                File clientDirectory = new File(fileDirectory);
                File[] files = clientDirectory.listFiles();
                String fileName = "";

                // If the length of the files array is not the same as the hashmap, a file has been added or deleted.
                // Otherwise, check if the bytes are different.
                if(files.length != cloudFiles.size())
                {
                    for(int i = 0; i < files.length; i++)
                    {
                        fileName = files[i].getName();

                        // If the cloudFiles hashmap does not have the file add it. Otherwise check if the file does 
                        // not exist. If so, delete it.
                        if(!cloudFiles.containsKey(fileName))
                        {
                            byte[] data = Files.readAllBytes(files[i].toPath());
                            FileData fd = new FileData(data, fileName, data.length);
                            fd.createSegments(fd.getData(), blockSize, Segment.Block);
                            cloudFiles.put(fd.getFileName(), fd);
                            listUpdated = true;
                        }

                        else
                        {
                            for(String j : cloudFiles.keySet())
                            {
                                fileDeleted = !fileName.equals(cloudFiles.get(j).getFileName());

                                if(fileDeleted)
                                {
                                    cloudFiles.remove(fileName);
                                    listUpdated = true;
                                }
                            }
                        }
                    }
                }

                else
                {
                    for(int i = 0; i < files.length; i++)
                    {
                        fileName = files[i].getName();

                        if(!Arrays.equals(Files.readAllBytes(files[i].toPath()), cloudFiles.get(fileName).getData()))
                        {
                            listUpdated = true;
                        }
                    }
                }

                if(listUpdated)
                {
                    for(int i = 0; i < cloudFiles.size(); i++)
                    {

                        System.out.println("Current Files:\n");
                        cloudFiles.forEach((k, v) -> System.out.printf("Name: %s\n", v.getFileName()));
                        listUpdated = false;
                    }
                }
            }

            int i = 0;
            int index = -1;
            SystemAction command = null;
            
            while(true)
            {
                System.out.println("What action do you want to perform?\nType:\nupload <FILE>,\ndownload <FILE>,\nedit <FILE>,\ndelete <FILE>,\n" +
                    "pause <UPLOAD/DOWNLOAD> <INDEX>,\nresume <UPLOAD/DOWNLOAD> <INDEX>,\ncancel <UPLOAD/DOWNLOAD> <INDEX>,\nor quit\n");
                try 
                {
                    String action = sc.nextLine();
                    actions = action.split(" ");
                    tcpm.sendMessageToServer(actions[0], 5000);

        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
*/