package cloudstorage.client;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
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

    @SuppressWarnings("unchecked")
    static<T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>)event;
    }


    public static void main(String[] args)
    {
        Set<String> fileEvents = new HashSet<String>();

        sc = new Scanner(System.in);

        ConcurrentHashMap<String, FileReader> readers = new ConcurrentHashMap<String, FileReader>();

        try
        {
            WatchService watcher = FileSystems.getDefault().newWatchService();

            // Get address of local host.
            address = InetAddress.getLocalHost();
            
            buffer = new byte[bufferSize];

            // Establish TCP socket connection
            Socket tcpSocket = new Socket(address, 2023);

            // Establish UDP socket connection.
            DatagramSocket udpSocket = new DatagramSocket();

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            SystemAction command = null;

            byte[] data = null;

            String localDir = System.getProperty("user.dir") + "/cloudstorage/client/files";

            Path clientDirectory = Paths.get(System.getProperty("user.dir") + "/cloudstorage/client/files");

            WatchKey key = null;

            while(true)
            {
                try
                {
                    key = clientDirectory.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                }

                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }

                for(WatchEvent<?> event : key.pollEvents())
                {
                    WatchEvent.Kind<?> kind = event.kind();

                    if(kind == OVERFLOW)
                    {
                        continue;
                    }

                    WatchEvent<Path> ev = cast(event);
                    Path fileName = ev.context();

                    try
                    {
                        Path child = clientDirectory.resolve(fileName);

                        data = getFileData(localDir + "/" + fileName.toString());

                        if((kind == ENTRY_CREATE || kind == ENTRY_MODIFY) && !fileEvents.contains(fileName.toString()))
                        {

                            if(readers.containsKey(fileName.toString()) && readers.get(fileName.toString()).getComplete())
                            {
                                readers.remove(fileName.toString());
                            }

                            if(readers.containsKey(fileName.toString()))
                            {
                                continue;
                            }

                            readers.put(fileName.toString(), new FileReader(data, fileName.toString(), data.length, SystemAction.Upload, tcpm, udpm, 2023, address));
                            readers.get(fileName.toString()).start();
                        }

                        else if(kind == ENTRY_DELETE)
                        {
                            if(data == null)
                            {
                                data = new byte[1];
                            }
                            readers.put(fileName.toString(), new FileReader(data, fileName.toString(), data.length, SystemAction.Delete, tcpm, udpm, 2023, address));
                            readers.get(fileName.toString()).start();                            
                        }
                    }

                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                boolean valid = key.reset();

                if(!valid)
                {
                    break;
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static byte[] getFileData(String fileName)
    {
            // Get file to transfer.
            File targetFile = new File(fileName);
            byte[] data = null;

            if(!targetFile.exists())
            {
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