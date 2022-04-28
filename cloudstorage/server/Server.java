package cloudstorage.server;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;

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
        port = 2023;

        // 4 MB buffer to receive data
        buffer = new byte[bufferSize];

        SQLManager manager = new SQLManager();

        manager.setDBConnection(ConnectionType.Server);

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

                ServerThread st = new ServerThread(tcpSocket, udpSocket, buffer, bufferSize, ++i);

                st.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}