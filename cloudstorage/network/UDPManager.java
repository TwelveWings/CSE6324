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

  /*
     * \brief closeSocket
     * 
     * Closes the UDP Socket
    */
    public void closeSocket()
    {
        udpSocket.close();
    }
    
  /*
     * \brief receiveDatagramPacket
     * 
     * Receives a datagram packet and sets the socket to timeout. This is used in the startup of the client
     * to ensure that a UDP socket is received.
     * 
     * \param buffer is the size of the buffer for the packet.
     * \param timeout is the timeout to ensure there are no synchronization issues.
     * 
     * Returns the packet
    */
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

  /*
     * \brief receivePacket
     * 
     * Receives a datagram packet and removes the timeout set in the startup. 
     * 
     * \param buffer is the size of the buffer for the packet.
     * \param timeout is the timeout to ensure there are no synchronization issues.
     * 
     * Returns the packet converted to a byte[].
    */
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

  /*
     * \brief sendPacket
     * 
     * Sends a datagram packet
     * 
     * \param data is the data being added to the packet.
     * \param targetAddress is the address the packet is being sent to.
     * \param targetPort is the port the packet is being sent to.
     * \param timeout is the timeout to ensure there are no synchronization issues.
    */
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

  /*
     * \brief sendEmptyPacket
     * 
     * Sends an empty datagram packet. Used in the startup of a client to connect to the server.
     * 
     * \param length is the length of the empty packet.
     * \param serverAddress is the address the packet is being sent to.
     * \param serverPort is the port the packet is being sent to.
    */
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
