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

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            int i = 0;
            while(true)
            {
                System.out.println("What action do you want to perform? (1 - Upload, 2 - Download, 3 - Edit, 4 - Delete, 0 - Quit)");

                try 
                {
                    String action = sc.next();

                    if(action.equals("0"))
                    {
                        break;
                    }

                    tcpm.sendMessageToServer(action, 5000);
    
                    switch(Integer.valueOf(action))
                    {
                        case 1:
                            //uploadFile();
                            ct = new ClientThread(tcpSocket, udpSocket, address,  buffer, bufferSize, ++i, Action.Upload);
                            ct.start();
                            break;
                        case 2:
                            //downloadFile();
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i, Action.Download);
                            ct.start();
                            break;
                        case 3:
                            //editFile();
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i, Action.Edit);
                            ct.start();
                            break;
                        case 4:
                            //deleteFile();
                            ct = new ClientThread(tcpSocket, udpSocket, address, buffer, bufferSize, ++i, Action.Delete);
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