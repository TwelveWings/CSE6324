package cloudstorage.network;

import java.net.*;
import javax.swing.JOptionPane;

public class UDPManager 
{
    public DatagramSocket udpSocket;

    public UDPManager(DatagramSocket socket)
    {
        udpSocket = socket;
    }

    public void closeSocket()
    {
        udpSocket.close();
    }
    
    public DatagramPacket receiveDatagramPacket(byte[] buffer, int timeout)
    {
        // Instantiate DatagramPacket object based on received data - rBuffer (received Buffer).
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        try
        {
            udpSocket.setSoTimeout(1000);

            // Receive file data from client program.
            udpSocket.receive(receivedPacket);

            // Sleep for specified timeout.
            Thread.sleep(timeout);
        }

        catch(SocketTimeoutException ste)
        {
            return null;
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public byte[] receivePacket(byte[] buffer, int timeout)
    {
        byte[] data = null;

        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            udpSocket.setSoTimeout(0);

            // Receive file name from client program.
            udpSocket.receive(receivedPacket);

            data = receivedPacket.getData().clone();

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return data;
    }


    public void sendPacket(byte[] data, InetAddress targetAddress, int targetPort, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);

            udpSocket.send(packet);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendEmptyPacket(int length, InetAddress serverAddress, int serverPort)
    {
        try
        {
            byte[] empty = new byte[length];
            DatagramPacket packet = new DatagramPacket(empty, length, serverAddress, serverPort);

            udpSocket.send(packet);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
