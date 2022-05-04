package cloudstorage.data;

import cloudstorage.enums.Segment;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class FileData 
{
    public List<byte[]> blocks;
    public List<byte[]> packets;
    public List<Integer> packetMap;
    public byte[] data;
    public String fileName;
    public int fileSize;

    public FileData()
    {
        data = null;
        fileName = "";
        fileSize = 0;
    }

    public FileData(byte[] d, String fn, int fs)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
    }

    public byte[] getData()
    {
        return data;
    }

    public void setData(byte[] d)
    {
        data = d;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fn)
    {
        fileName = fn;
    }

    public int getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(int fs)
    {
        fileSize = fs;
    }

    public List<byte[]> getBlocks()
    {
        return blocks;
    }

    public void setBlocks(List<byte[]> b)
    {
        blocks = b;
    }

    public List<byte[]> getPackets()
    {
        return packets;
    }

    public void setPackets(List<byte[]> p)
    {
        packets = p;
    }

    public int[] findChange(List<byte[]> currData, List<byte[]> newData)
    {
        int[] maxMin = { -1, -1 };

        if(currData.size() < newData.size())
        {
            for(int i = 0; i < (newData.size() - (newData.size() - currData.size())) ; i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)) && maxMin[0] == -1)
                {
                    maxMin[0] = i;
                }

                else if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    maxMin[1] = i;
                }
            }
        }

        else if(currData.size() > newData.size())
        {
            for(int i = 0; i < (currData.size() - (currData.size() - newData.size())) ; i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)) && maxMin[0] == -1)
                {
                    maxMin[0] = i;
                }

                else if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    maxMin[1] = i;
                }
            }       
        }

        else
        {
            for(int i = 0; i < currData.size() ; i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)) && maxMin[0] == -1)
                {
                    maxMin[0] = i;
                }

                else if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    maxMin[1] = i;
                }
            }           
        }

        // If the min value was changed but the max was not only one block changed.
        if(maxMin[0] != -1 && maxMin[1] == -1)
        {
            maxMin[1] = maxMin[0];
        }

        return maxMin;
    }
    
    public byte[] combinePacketData(byte[][] data, int iterations)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try
        {
            for(int i = 0; i < iterations; i++)
            {
                bos.write(data[i]);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public byte[] combineBlockData(List<byte[]> data, int iterations)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try
        {
            for(int i = 0; i < iterations; i++)
            {
                bos.write(data.get(i));
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public void createSegments(byte[] data, int size, Segment type)
    {
        List<byte[]> segments = new ArrayList<byte[]>();

        // Create a temporary (temp) byte array to hold block data and an empty byte array to check
        //  when the array has been emptied.
        byte[] temp = new byte[size];
        byte[] empty = new byte[size];

        // Used to see how much of the file data has been processed.
        int remainingData = data.length;

        // For each byte in the file, add it to the temp byte array. If the temp array gets filled add
        // it to the dataBlocks ArrayList. Then clear it to begin processing the next block.
        for(int i = 0; i < data.length; i++)
        {
            // If the array is empty and remaining data is not equal to or greater than the buffer size, 
            // the temp array needs to be sized to ensure only actual data is included in the block.
            if(Arrays.equals(temp, empty) && remainingData < size)
            {
                temp = new byte[remainingData];
            }

            temp[i % size] = data[i];

            // Check to see if the temp array has reached maximum capacity. If so, add the block to the
            // dataBlocks ArrayList and clear it the temp array.
            if(i > 0 && (((i + 1) % size == 0) || (i == data.length - 1 && remainingData < size)))
            {
                // If the segment is a packet add extra information to detail which block the packet
                // corresponds to.
                if(type == Segment.Packet)
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] block = new byte[2];

                    //System.out.printf("SS: %d\n", segments.size() % 129);

                    block[0] = (byte)(segments.size() / 129);
                    block[1] = (byte)(segments.size() % 129);
                    
                    try
                    {
                        bos.write(block);
                        bos.write(temp);
                    }

                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                    byte[] p = bos.toByteArray();

                    segments.add(p);
                    temp = new byte[size];
                }

                else
                {
                   // System.out.println(remainingData);
                    segments.add(temp);
                    temp = new byte[size];
                }
            }

            remainingData--;
        }

        if(type == Segment.Block)
        {
            setBlocks(new ArrayList<>(segments));
        }

        else
        {
            setPackets(new ArrayList<>(segments));
        }

        segments.clear();
    }

    public byte[] stripIdentifier(byte[] data)
    {
        byte[] temp = new byte[data.length - 2];

        for(int i = 2; i < data.length; i++)
        {
            temp[i - 2] = data[i];
        }

        return temp;
    }

    public byte[] stripPadding(byte[] data, int newSize)
    {
        byte[] temp = new byte[newSize];

        for(int i = 0; i < temp.length; i++)
        {
            temp[i] = data[i];
        }

        return temp;
    }
}
