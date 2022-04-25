package cloudstorage.client;

import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;

public class Client
{
    public static byte[] buffer;
    public static InetAddress address;
    public static int port;
    public static final int blockSize = 1024 * 1024 * 4;
    public static final int bufferSize = 65507;
    public static Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);
        List<ClientThread> uploads = new ArrayList<ClientThread>();
        List<ClientThread> downloads = new ArrayList<ClientThread>();

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

            String[] actions = null;

            tcpm = new TCPManager(tcpSocket);
            udpm = new UDPManager(udpSocket);

            int i = 0;
            int index = -1;
            SystemAction command = null;
            
            while(true)
            {
                System.out.println("What action do you want to perform? (Type: upload <FILE>, download <FILE>, edit <FILE>, delete <FILE> or quit)");

                try 
                {
                    String action = sc.nextLine();

                    actions = action.split(" ");

                    tcpm.sendMessageToServer(actions[0], 5000);

                    if(action.toLowerCase().equals("quit"))
                    {
                        tcpm.closeSocket();
                        udpm.closeSocket();
                        
                        System.out.println("Program terminated.");
                        return;
                    }

                    switch(actions[0].toLowerCase())
                    {
                        case "upload":
                            command = SystemAction.Upload;
                            break;
                        case "download":
                            command = SystemAction.Download;
                            break;
                        case "edit":
                            command = SystemAction.Edit;
                            break;
                        case "delete":
                            command = SystemAction.Delete;
                            break;
                        case "pause":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).setIsPaused(true);
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                downloads.get(index).setIsPaused(true);
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        case "resume":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).setIsPaused(false);
                                uploads.get(index).resumeThread();
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                downloads.get(index).setIsPaused(false);
                                downloads.get(index).resumeThread();
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        case "cancel":
                            command = null;
                            index = Integer.valueOf(actions[2]);

                            if(actions[1].toLowerCase().equals("upload") && index < uploads.size())
                            {
                                uploads.get(index).interrupt();
                            }

                            else if(actions[1].toLowerCase().equals("download") && index < downloads.size())
                            {
                                uploads.get(index).interrupt();
                            }

                            else
                            {
                                System.out.println("That command does not exist.");
                            }

                            break;
                        default:
                            System.out.println("Invalid action. Please try again.");
                            break;
                    }

                    if(command != null)
                    {
                        ct = new ClientThread(tcpSocket, udpSocket, address,  buffer, bufferSize, ++i, actions[1], command);

                        if(command == SystemAction.Upload)
                        {
                            uploads.add(ct);
                        }

                        else if(command == SystemAction.Download)
                        {
                            downloads.add(ct);
                        }

                        ct.start();
                    }
                }

                catch(InputMismatchException ime)
                {
                    continue;
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}