package cloudstorage.network;

import java.io.*;
import java.net.*;

public class TCPManager 
{
    public Socket tcpSocket;

    public TCPManager(Socket socket)
    {
        tcpSocket = socket;
    }

    public void closeSocket()
    {
        try 
        {
            tcpSocket.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public String receiveMessageFromClient(int timeout)
    {
        String message = "";

        try
        {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            message = fromClient.readLine();

            Thread.sleep(timeout);
        }

        catch(SocketException se)
        {
            System.out.println("Client closed unexpectedly.");
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
            System.out.println("Server failed to send message.");

            e.printStackTrace();
        }
    }

    public String receiveMessageFromServer(int timeout)
    {
        String message = "";

        try
        {
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            message = fromServer.readLine();

            Thread.sleep(timeout);
        }

        catch(SocketException se)
        {
            System.out.println("The server was disconnected. Program terminated.");
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return message;
    }

    public void sendMessageToServer(String message, int timeout)
    {
        try
        {
            PrintWriter toServer = new PrintWriter(tcpSocket.getOutputStream(), true);
            toServer.println(message);

            Thread.sleep(timeout);
        }

        catch(Exception e)
        {
            System.out.println("Client failed to send message.");
    
            e.printStackTrace();
        }
    }

}

