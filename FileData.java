import java.util.*;

public class FileData 
{
    public List<byte[]> blocks;
    public List<byte[]> packets;
    public byte[] data;
    public String fileName;
    public int fileSize;

    public FileData(byte[] d, String fn, int fs)
    {
        data = d;
        fileName = fn;
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

    public void createSegments(byte[] data, int size, Segment type)
    {
        List<byte[]> segments = new ArrayList<byte[]>();

        // Create a temporary (temp) byte array to hold block data and an empty byte array to check
        //  when the array has been emptied.
        byte[] temp = new byte[size];
        byte[] empty = new byte[size];

        // Used to see how much of the file data has been processed.
        int remainingData = data.length;
        
        // For each byte in the file, add it to the temp byte array. If the temp array gets filled add it
        // to the dataBlocks ArrayList. Then clear it to begin processing the next block.
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
            if(i > 0 && ((i + 1 % size == 0) || (i == data.length - 1 && remainingData < size)))
            {
                segments.add(temp);
                temp = new byte[size];
            }

            remainingData--;
        }

        if(type == Segment.Block)
        {
            setBlocks(segments);
        }

        else
        {
            setPackets(segments);
        }
    }
}
