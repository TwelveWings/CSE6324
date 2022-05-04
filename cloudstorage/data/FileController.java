package cloudstorage.data;

import cloudstorage.control.*;
import cloudstorage.data.*;
import cloudstorage.enums.*;
import cloudstorage.network.*;
import java.net.*;
import java.util.*;

public class FileController 
{
    public BoundedBuffer boundedBuffer;
    public FileData fileData;
    public InetAddress targetAddress;
    public String fileName;
    public Synchronizer sync;
    public Synchronizer uploadSync;
    public TCPManager tcpm;
    public UDPManager udpm;
    public int targetPort;

    public FileController(FileData fd, TCPManager tcp, UDPManager udp, Synchronizer s, Synchronizer us, 
        BoundedBuffer bb, InetAddress a, int p)
    {
        fileData = fd;
        tcpm = tcp;
        udpm = udp;
        sync = s;
        uploadSync = us;
        boundedBuffer = bb;
        targetAddress = a;
        targetPort = p;
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

            // System.out.printf("PACKET #: %d\n", packetsCreated.size());

            for(int j = 0; j < packetsCreated.size(); j++)
            {
                sync.checkIfPaused();

                boundedBuffer.deposit(packetsCreated.get(j));

                SendThread st = new SendThread(udpm, packetsCreated, ConnectionType.Client, 
                    Protocol.UDP, targetPort, targetAddress, boundedBuffer);
                st.start();

                try
                {
                    st.join();
                }

                catch (Exception e)
                {

                }
            }
        }

        uploadSync.blockedFiles.replace(fileName, false);
    }

    synchronized public void delete()
    {
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

        uploadSync.blockedFiles.replace(fileName, false);
    }
}
