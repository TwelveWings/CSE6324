package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import cloudstorage.views.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class FileController 
{
    public BoundedBuffer boundedBuffer;
    public ClientUI ui;
    public Date date = new Date(System.currentTimeMillis());
    public FileData fileData;
    public InetAddress targetAddress;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public String fileName;
    public String timestamp = formatter.format(date);
    public Synchronizer sync;
    public Synchronizer uploadSync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int targetPort;

    public FileController(FileData fd, TCPManager tcp, UDPManager udp, Synchronizer s, Synchronizer us, 
        BoundedBuffer bb, InetAddress a, int p, ClientUI u)
    {
        fileData = fd;
        tcpm = tcp;
        udpm = udp;
        sync = s;
        uploadSync = us;
        boundedBuffer = bb;
        targetAddress = a;
        targetPort = p;
        ui = u;
    }

    synchronized public void upload()
    {
        // Split file into blocks
        fileData.createSegments(fileData.getData(), 1024 * 1024 * 4, Segment.Block);

        List<byte[]> blocksCreated = fileData.getBlocks();

        tcpm.sendMessageToServer("upload", 1000);
        tcpm.sendMessageToServer(fileData.getFileName(), 1000);
        tcpm.sendMessageToServer(String.valueOf(fileData.getFileSize()), 1000);
        tcpm.sendMessageToServer(String.valueOf(blocksCreated.size()), 1000);

        for(int i = 0; i < blocksCreated.size(); i++)
        {
            sync.checkIfPaused();
            
            // Read the block and create packets
            fileData.createSegments(blocksCreated.get(i), 65505, Segment.Packet);

            List<byte[]> packetsCreated = fileData.getPackets();

            tcpm.sendMessageToServer(String.valueOf(packetsCreated.size()), 1000);

            ui.textfield1.append(" [" + timestamp + "] Transmitting data to server...\n");

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                sync.checkIfPaused();

                boundedBuffer.deposit(packetsCreated.get(j));

                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Client, 
                    Protocol.UDP, targetPort, targetAddress, boundedBuffer);
                st.start();

                ui.textfield1.append(" [" + timestamp + "] NUM_PACKETS : " + 
                    String.valueOf(packetsCreated.get(j)[1] + 1) + " OUT OF " + 
                    String.valueOf(packetsCreated.size()) + "\n");

                ui.textfield1.append(" [" + timestamp + "] NUM_BLOCKS : " + 
                    String.valueOf(i + 1) + " OUT OF " + 
                    String.valueOf(blocksCreated.size()) + "\n");

                try
                {
                    st.join();
                }

                catch (Exception e)
                {

                }
            }
        }

        ui.textfield1.append(" [" + timestamp + "] Data transmission for " + fileData.getFileName() + 
            " complete.\n");

        uploadSync.blockedFiles.replace(fileData.getFileName(), false);

    }

    synchronized public void delete()
    {
        ui.textfield1.append(" [" + timestamp + "] " + fileData.getFileName() + " deleted. Updating " +
            "server.\n");

        tcpm.sendMessageToServer("delete", 1000);

        try
        {
            // Send file name to delete on server.
            tcpm.sendMessageToServer(fileData.getFileName(), 1000);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        ui.textfield1.append(" [" + timestamp + "] Complete.\n");


        uploadSync.blockedFiles.replace(fileData.getFileName(), false);
    }
}
