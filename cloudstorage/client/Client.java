package cloudstorage.client;

import cloudstorage.data.FileData;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class Client
{
    public static byte[] buffer;
    public static InetAddress address;
    public static int port;
    public static final int blockSize = 1024 * 1024 * 4;
    public static final int bufferSize = 65536;
    public static Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);

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
            while(true)
            {
                System.out.println("What action do you want to perform? (Type: upload <FILE>, download <FILE>, edit <FILE>, delete <FILE> or quit)");

                try 
                {
                    String action = sc.nextLine();

                    if(action.toLowerCase().equals("quit"))
                    {
                        break;
                    }

                    actions = action.split(" ");

                    tcpm.sendMessageToServer(action, 5000);
    
                    switch(actions[0])
                    {
                        case "upload":
                            ct = new ClientThread(tcpSocket, udpSocket, address,  buffer, bufferSize, ++i, actions[1], Action.Upload);
                            ct.start();
                            break;
                        case "download":
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i,  actions[1], Action.Download);
                            ct.start();
                            break;
                        case "edit":
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i,  actions[1], Action.Edit);
                            ct.start();
                            break;
                        case "delete":
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i,  actions[1], Action.Delete);
                            ct.start();
                            break;
                        default:
                            System.out.println("Invalid action. Please try again.");
                            break;
                    }
                }

                catch(InputMismatchException ime)
                {
                    continue;
                }
            }

            tcpm.closeSocket();
            udpm.closeSocket();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}