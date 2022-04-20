import java.net.*;
import java.util.*;

public class Server
{
    public static DatagramSocket udpSocket;
    public static byte[] buffer;
    public static int port;
    public static final int bufferSize = 65535;
    public static Scanner sc;
    public static ServerSocket serverSocket;
    public static Socket tcpSocket;

    public static void main(String[] args)
    {
        port = 17;

        // 4 MB buffer to receive data
        buffer = new byte[bufferSize];

        SQLManager manager = new SQLManager();

        List<ServerThread> sts = new ArrayList<ServerThread>();

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

            while(true)
            {
                tcpSocket = serverSocket.accept();

                TCPManager tcpm = new TCPManager(tcpSocket);
                UDPManager udpm = new UDPManager(udpSocket);

                sts.add(new ServerThread(tcpm, udpm, buffer, bufferSize, sts.size()));
                sts.get(sts.size() - 1).start();
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}