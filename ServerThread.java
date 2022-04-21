import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread extends Thread
{
    public Action action;
    public static byte[] buffer;
    public DatagramSocket udpSocket;
    public Socket tcpSocket;
    public Scanner sc;
    public static TCPManager tcpm;
    public static UDPManager udpm;
    public int ID;
    public static final int blockSize = 1024 * 1024 * 4;
    public static int bufferSize;

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
    }

    public ServerThread(Socket tcp, DatagramSocket udp, byte[] b, int bs, int tID, Action a)
    {
        tcpSocket = tcp;
        udpSocket = udp;
        ID = tID;
        buffer = b;
        bufferSize = bs;
        action = a;
    }

    public void run()
    {
        tcpm = new TCPManager(tcpSocket);
        udpm = new UDPManager(udpSocket);

        String action = tcpm.receiveMessageFromClient();

        System.out.println("System " + String.valueOf(ID));

        while(true)
        {
            switch(action)
            {
                case "1":
                    uploadFile();
                    break;
                case "2":
                    downloadFile();
                    break;
                case "3":
                    editFile();
                    break;
                case "4":
                    deleteFile();
                    break;
            }

            action = tcpm.receiveMessageFromClient();

            Arrays.fill(buffer, (byte)0);
        }
    }

    synchronized public static void deleteFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            int fileDeleted = manager.deleteFile(fileName);

            if(fileDeleted == 0)
            {
                tcpm.sendMessageToClient("File does not exist in server.", 5000);
            }

            else if(fileDeleted == 1)
            {
                tcpm.sendMessageToClient("File deleted successfully!", 5000);
            }

            else
            {
                tcpm.sendMessageToClient("Error occurred. File not deleted.", 5000);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    synchronized public static void downloadFile()
    {
        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            DatagramPacket receivedMessage = udpm.receivePacketFromClient(buffer);

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            if(files.get(fileName) != null)
            {
                tcpm.sendMessageToClient(String.valueOf(files.get(fileName).fileSize), 5000);

                udpm.sendPacketToClient(files.get(fileName).data, 
                    receivedMessage.getAddress(), receivedMessage.getPort(), 5000);
            }

            else
            {
                String fileSize = String.valueOf(0);

                // Send file size using TCP
                tcpm.sendMessageToClient(fileSize, 5000);

                tcpm.sendMessageToClient("File does not exist in server.", 5000);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        manager.closeConnection();
    }

    synchronized public static void editFile()
    {
        return;
    }

    synchronized public static void uploadFile()
    {
        try
        {
            String fileName = tcpm.receiveMessageFromClient();

            SQLManager manager = new SQLManager(fileName);
            manager.setDBConnection();

            Arrays.fill(buffer, (byte)0);

            List<byte[]> packets = new ArrayList<byte[]>();
            byte[] dataBuffer = null;

            int numPackets = Integer.valueOf(tcpm.receiveMessageFromClient());
            int fileSize = 0;

            for(int i = 0; i < numPackets; i++)
            {
                int packetSize = Integer.valueOf(tcpm.receiveMessageFromClient());

                fileSize += packetSize;

                dataBuffer = new byte[packetSize];

                // Receive block from client.
                DatagramPacket receivedMessage = udpm.receivePacketFromClient(dataBuffer);

                packets.add(receivedMessage.getData());
            }

            byte[] fileData = new byte[fileSize];

            int startPosition = 0;

            // Loop through each block. Add blocks to fileData.
            for(int i = 0; i < packets.size(); i++)
            {
                for(int j = startPosition; j < fileSize - startPosition; j++)
                {
                    fileData[j] = packets.get(i)[j - startPosition];
                }

                // New start position is based on the end of the last blocks. 
                startPosition += packets.get(i).length;
            }

            ConcurrentHashMap<String, FileData> files = manager.selectAllFiles();

            int resultCode = 0;
            
            System.out.println("Test");

            if(files.get(fileName) == null)
            {
                resultCode = manager.insertData(fileData, fileSize);

                System.out.printf("SENDING: %d", resultCode);

                tcpm.sendMessageToClient(String.valueOf(resultCode), 5000);

                String message = (resultCode == 1) ? "File uploaded successfully!" : "Error occurred. File not uploaded.";

                System.out.printf("SENDING: %s", message);

                tcpm.sendMessageToClient(message, 5000);
            }

            else
            {
                tcpm.sendMessageToClient(String.valueOf(resultCode), 5000);

                tcpm.sendMessageToClient("File already exists.", 5000);
                
                int clientResponse = Integer.valueOf(tcpm.receiveMessageFromClient());

                if(clientResponse == 1)
                {
                    manager.updateFileByName(fileName, fileData);

                    System.out.printf("SENDING: message");

                    tcpm.sendMessageToClient("File uploaded successfully!", 5000);
                }
            }

            // Close connection to DB
            manager.closeConnection();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
