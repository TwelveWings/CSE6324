import java.util.*;

public class FileData 
{
    public List<byte[]> blocks;
    public byte[] data;
    public String fileName;
    public static int bufferSize;
    public int fileSize;

    public FileData(byte[] d, String fn, int fs)
    {
        data = d;
        fileName = fn;
        fileSize = fs;
    }

    public FileData(byte[] d, String fn, int bs, int fs)
    {
        data = d;
        fileName = fn;
        bufferSize = bs;
        fileSize = fs;
    }

    public void createBlocks(byte[] data)
    {
        List<byte[]> dataBlocks = new ArrayList<byte[]>();

        byte[] temp = new byte[bufferSize];

        for(int i = 0; i < data.length; i++)
        {
            temp[i % bufferSize] = data[i];

            if(i > 0 && ((i + 1) % bufferSize) == 0)
            {
                dataBlocks.add(temp);
                Arrays.fill(temp, (byte)0);
            }
        }

        blocks = dataBlocks;
    }

}
