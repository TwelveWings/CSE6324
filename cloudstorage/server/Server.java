package cloudstorage.server;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;

public class Server
{
    public static DatagramSocket udpSocket;
    public static byte[] buffer;
    public static int port;
    public static final int bufferSize = 65507;
    public static ServerSocket serverSocket;
    public static Socket tcpSocket;

    public static void main(String[] args)
    {
        System.out.println("Server running...");
        port = 2023;

        List<ClientData> clients = new ArrayList<ClientData>();

        // 4 MB buffer to receive data
        buffer = new byte[bufferSize];

        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        // If user specifies new drop table.
        if(args.length > 0 && args[0].equals("new"))
        {
            manager.dropTable();
        }

        manager.createTable();

        manager.closeConnection();

        try
        {
            // Establish TCP connection with port
            serverSocket = new ServerSocket(port);

            // Establish UPD connection with port
            udpSocket = new DatagramSocket(port);

            int i = 0;

            while(true)
            {
                tcpSocket = serverSocket.accept();

                System.out.println(tcpSocket.getPort());
                System.out.println(tcpSocket.getInetAddress());

                i++;

                clients.add(new ClientData(i, tcpSocket.getPort(), tcpSocket.getInetAddress(), tcpSocket, udpSocket));

                ServerThread st = new ServerThread(tcpSocket, udpSocket, buffer, bufferSize, i, clients);

                st.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}