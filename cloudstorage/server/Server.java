package cloudstorage.server;

import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.text.SimpleDateFormat;
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
        ServerUI ui = new ServerUI();

        System.out.println("Opening Server GUI...");

        ui.appendToLog("Server is running...");

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

            UDPManager udpm = new UDPManager(udpSocket);

            int i = 0;

            while(true)
            {
                tcpSocket = serverSocket.accept();

                TCPManager tcpm = new TCPManager(tcpSocket);

                ui.appendToLog(String.format("TCP Connection with (port/address): %d/%s", tcpSocket.getPort(), tcpSocket.getInetAddress()));
                
                // A datagram packet must be received from the client in order to establish which port
                // is being used.
                DatagramPacket connector = null;
                
                while(connector == null)
                {
                    connector = udpm.receiveDatagramPacket(buffer, 1000);

                    if(connector == null)
                    {
                        tcpm.sendMessageToClient("-1", 1000);
                    }
                }

                tcpm.sendMessageToClient("1", 1000);

                int udpPort = connector.getPort();
                InetAddress udpAddress = connector.getAddress();

                ui.appendToLog(String.format("UDP Connection with (port/address): %d/%s", udpPort, udpAddress));
                
                i++;

                clients.add(new ClientData(i, tcpSocket.getPort(), tcpSocket.getInetAddress(), udpPort,
                    udpAddress, tcpSocket, udpSocket));

                ServerThread st = new ServerThread(tcpSocket, udpSocket, buffer, bufferSize, i, clients,
                    ui);

                st.start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}