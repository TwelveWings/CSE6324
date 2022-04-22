import java.net.*;

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
    
    public DatagramPacket receivePacketFromClient(byte[] rBuffer)
    {
        // Instantiate DatagramPacket object based on received data - rBuffer (received Buffer).
        DatagramPacket receivedPacket = new DatagramPacket(rBuffer, rBuffer.length);

        try
        {
            // Receive file data from client program.
            udpSocket.receive(receivedPacket);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public DatagramPacket receivePacketFromServer(byte[] buffer)
    {
        // Instantiate DatagramPacket object based on buffer.
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        try
        {
            // Receive file name from client program.
            udpSocket.receive(receivedPacket);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return receivedPacket;
    }


    public void sendPacketToClient(byte[] data, InetAddress clientAddress, int clientPort, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);

            udpSocket.send(packet);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendPacketToServer(byte[] data, InetAddress serverAddress, int serverPort, int timeout)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);

            udpSocket.send(packet);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
