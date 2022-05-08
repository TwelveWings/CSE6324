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
    public List<byte[]> unmodifiedBlocks;
    public List<Integer> changedIndices;
    public boolean fileIsModified;
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
        fileIsModified = false;
    }

    /*
     * \brief getData
     * 
     * Retrieves the value currently assigned to data.
     * 
     * Returns the byte[] value of data.
    */
    public byte[] getData()
    {
        return data;
    }

    /*
     * \brief setData
     * 
     * Assigns a value to the object's data variable.
     * 
     * \param d is the new byte[] value being assigned to data.
    */
    public void setData(byte[] d)
    {
        data = d;
    }

    /*
     * \brief getFileName
     * 
     * Retrieves the value currently assigned to fileName
     * 
     * Returns the String value of fileName.
    */
    public String getFileName()
    {
        return fileName;
    }

    /*
     * \brief setFileName
     * 
     * Assigns a value to the object's fileName variable.
     * 
     * \param fn is the new String value being assigned to fileName.
    */
    public void setFileName(String fn)
    {
        fileName = fn;
    }

    /*
     * \brief getFileSize
     * 
     * Retrieves the value currently assigned to fileSize
     * 
     * Returns the int value of fileSize.
    */
    public int getFileSize()
    {
        return fileSize;
    }

    /*
     * \brief setFileSize
     * 
     * Assigns a value to the object's fileSize variable.
     * 
     * \param fs is the new int value being assigned to fileize.
    */
    public void setFileSize(int fs)
    {
        fileSize = fs;
    }

    /*
     * \brief getBlocks
     * 
     * Retrieves the value currently assigned to blocks
     * 
     * Returns the List<byte[]> value of blocks.
    */
    public List<byte[]> getBlocks()
    {
        return blocks;
    }

    /*
     * \brief setBlocks
     * 
     * Assigns a value to the object's blocks variable.
     * 
     * \param b is the new List<byte[]> value being assigned to blocks.
    */
    public void setBlocks(List<byte[]> b)
    {
        blocks = b;
    }

    /*
     * \brief getUnmodifiedBlocks
     * 
     * Retrieves the value currently assigned to unmodifiedBlocks
     * 
     * Returns the List<byte[]> value of unmodifiedBlocks
    */
    public List<byte[]> getUnmodifiedBlocks()
    {
        return unmodifiedBlocks;
    }

    /*
     * \brief setUnmodifiedBlocks
     * 
     * Assigns a value to the object's unmodifiedBlocks variable.
     * 
     * \param b is the new List<byte[]> value being assigned to unmodifiedBlocks.
    */
    public void setUnmodifiedBlocks(List<byte[]> b)
    {
        unmodifiedBlocks = b;
    }

    /*
     * \brief getPackets
     *
     * Retrieves the value currently assigned to packets
     * 
     * Returns the List<byte[]> value of packets
    */
    public List<byte[]> getPackets()
    {
        return packets;
    }

    /*
     * \brief setPackets
     * 
     * Assigns a value to the object's packets variable.
     * 
     * \param p is the new List<byte[]> value being assigned to packets.
    */
    public void setPackets(List<byte[]> p)
    {
        packets = p;
    }

    /*
     * \brief getChanges
     * 
     * Retrieves the value currently assigned to changedIndices
     * 
     * Returns the List<Integer> value of changedIndices
    */
    public List<Integer> getChanges()
    {
        return changedIndices;
    }

    /*
     * \brief isFileModified
     * 
     * Retrieves the value currently assigned to fileIsModified
     * 
     * Returns the boolean value of fileIsModified
    */
    public boolean isFileModified()
    {
        return fileIsModified;
    }

    /*
     * \brief setDeltaSyncBLocks
     * 
     * Uses findChange method to determine which indices have changed from file modification and updates
     * unmodifiedBlocks and blocks based on this.
     * 
     * Returns a boolean value to determine if any changes occurred.
    */
    public boolean setDeltaSyncBlocks()
    {
        changedIndices = findChange(unmodifiedBlocks, blocks);

        // If there are no changes return false to indicate as such. Otherwise, change blocks
        // to use only the changes.
        if(changedIndices.size() > 0)
        {
            fileIsModified = true;
            List<byte[]> changedBlocks = new ArrayList<byte[]>();

            // Loop through all the changed indices and add them to changedBlocks. Use the absolute
            // value, since numbers can be negative.
            for(int i = 0; i < changedIndices.size(); i++)
            {
                changedBlocks.add(blocks.get(Math.abs(changedIndices.get(i))));
            }

            // Set the unmodified blocks to be the blocks of the new file before blocks get overridden
            // with the changed blocks.
            setUnmodifiedBlocks(blocks);

            // Set blocks to use only the changed blocks.
            blocks = changedBlocks;

            return true;
        }

        return false;
    }

    /*
     * \brief findChange
     * 
     * Loops through the data of the new file to determine if there has been any changes.
     * 
     * \param currData is the List<byte[]> that existed before the registered event.
     * \param newData is the List<byte[]> that exists after the registered event.
     * 
     * Returns a list of indices that have been changed.
    */
    public List<Integer> findChange(List<byte[]> currData, List<byte[]> newData)
    {
        List<Integer> changes = new ArrayList<Integer>();

        if(currData.size() > newData.size())
        {
            // If the current data is greater than the new data (file size has decreased), loop through
            // the current data bound by the size of the new data to see if tehre are any changes.
            for(int i = 0; i < newData.size(); i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    changes.add(i);
                }                  
            }

            // Add all the deleted blocks to the list of changes. Make them negative to denote deletion.
            for(int i = newData.size(); i < currData.size(); i++)
            {
                changes.add(i * -1);
            }
        }

        else if(currData.size() < newData.size())
        {
            // If the current data is smaller than the new data (file size has increased), loop through
            // the current data to see if there are any changes. 
            for(int i = 0; i < currData.size(); i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    changes.add(i);
                }                 
            }

            // Add all the new blocks to the list of changes.
            for(int i = currData.size(); i < newData.size(); i++)
            {
                changes.add(i);
            }
        }

        else
        {
            // If the size is unchanged, loop through data to see which block differ.
            for(int i = 0; i < currData.size(); i++)
            {
                if(!Arrays.equals(currData.get(i), newData.get(i)))
                {
                    changes.add(i);
                }               
            }
        }

        return changes;
    }
    
    /*
     * \brief combinePacketData
     * 
     * Combines all packet data after the packets have been received.
     * 
     * \param data is a jagged array used to collect the packets into a single data structure.
     * \param iterations is the number of packets that have been put into data.
     * 
     * Returns the data combined into a single byte[]
    */
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

    /*
     * \brief combinePacketData
     * 
     * Combines all block data after all the packets in each block have been received.
     * 
     * \param data is a List<byte[]> used to collect the blocks into a data structure.
     * \param iterations is the number of blocks that have been put into data.
     * 
     * Returns the data combined into a single byte[]
    */
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

    /*
     * \brief createSegments
     * 
     * Divides the FileData object's data into their respective segment type based on the size. For example,
     * if the segment type is Segment.Block, the data will be divided into blocks equal to the size.
     * 
     * \param data is the data being segmented.
     * \param size is the size of the segments that the data will be divided into.
     * \param type is the type of segment that is being created.
    */
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

    /*
     * \brief stripIdentifier
     * 
     * Removes the first two bytes from an array.
     * 
     * \param data is the data that is having bytes removed.
    */
    public byte[] stripIdentifier(byte[] data)
    {
        byte[] temp = new byte[data.length - 2];

        for(int i = 2; i < data.length; i++)
        {
            temp[i - 2] = data[i];
        }

        return temp;
    }

    /*
     * \brief stripPadding
     * 
     * Removes the any excess data based on the newSize.
     * 
     * \param data is the data that is having bytes removed.
     * \param newSize is the size of the data after having data removed.
    */
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
