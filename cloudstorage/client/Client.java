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

        // Concurrent hashmap to store all Reader threads
        ConcurrentHashMap<String, FileReader> readers = new ConcurrentHashMap<String, FileReader>();

        try
        {
            // Watcher service to be used to watch changes in the specified directory.
            WatchService watcher = FileSystems.getDefault().newWatchService();

            // Get address of local host.
            address = InetAddress.getLocalHost();
            
            // Establish TCP socket connection
            Socket tcpSocket = new Socket(address, 2023);

            // Establish UDP socket connection.
            DatagramSocket udpSocket = new DatagramSocket();

            // TCP and UDP helper objects to send and receive messages and packets.
            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            // Bytes of file being read
            byte[] data = null;

            // The local directory that is being watched and will be used to synchronize with the server.
            String localDir = System.getProperty("user.dir") + "/cloudstorage/client/files";

            // Local directory converted to a Path.
            Path clientDirectory = Paths.get(localDir);

            // Watch key will keep track of ENTRY_CREATE, ENTRY_DELETE, and ENTRY MODIFY events.
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

                        Thread.sleep(1000);

                        // If the event is a create or modify event begin "upload" synchronization
                        if((kind == ENTRY_CREATE || kind == ENTRY_MODIFY) && !fileEvents.contains(fileName.toString()))
                        {
                            // When a file is added to a directory, it creates a ENTRY_CREATE event and an ENTRY_MODIFY event in rapid succession because
                            // the file is created and then its timestamp is modified. As such, if a thread is still processing a request prevent the system
                            // from creating a new thread. Otherwise, remove the thread from the hashmap and allow it to create a new thread.
                            if(readers.containsKey(fileName.toString()) && readers.get(fileName.toString()).getComplete())
                            {
                                readers.remove(fileName.toString());
                            }

                            if(readers.containsKey(fileName.toString()))
                            {
                                continue;
                            }

                            readers.put(fileName.toString(), new FileReader(fileName.toString(), SystemAction.Upload, tcpm, udpm, 2023, address));
                            readers.get(fileName.toString()).start();
                        }

                        else if(kind == ENTRY_DELETE)
                        {
                            readers.put(fileName.toString(), new FileReader(fileName.toString(), SystemAction.Delete, tcpm, udpm, 2023, address));
                            readers.get(fileName.toString()).start();                            
                        }
                    }

                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                // Reset the key. This is essential according to Oracle API.
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
}