import java.io.*;
import java.net.*;

public class TCPManager 
{
    public static Socket tcpSocket;

    public TCPManager(Socket socket)
    {
        tcpSocket = socket;
    }

    public String receiveMessageFromClient()
    {
        String message = "";

        try
        {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

            message = fromClient.readLine();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return message;
    }    

    public void sendMessageToClient(String message, int timeout)
    {
        try
        {
            PrintWriter toClient = new PrintWriter(tcpSocket.getOutputStream(), true);
        
            toClient.println(message);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}

