package cloudstorage.control;

import java.net.*;

public class BoundedBuffer 
{
    public int fullSlots = 0;
    public int capacity = 0;
    public byte[][] buffer = null;
    public int in = 0;
    public int out = 0;
    public volatile boolean fileDownloading;
    public volatile boolean fileUploading;

    public BoundedBuffer(int c, boolean fd, boolean fu)
    {
        capacity = c;
        fileDownloading = fd;
        fileUploading = fu;
        buffer = new byte[c][];
    }

    /*
     * \brief setFileUploading
     * 
     * Assigns a value to the object's fileUploading variable.
     * 
     * \param u is the new boolean value being assigned to fileUploading.
    */
    public void setFileUploading(boolean fu)
    {
        fileUploading = fu;
    }

    /*
     * \brief getFileUploaded
     * 
     * Retreives the value currently assigned to fileUploading.
     * 
     * Returns the boolean value of fileUploading.
    */
    public boolean getFileUploading()
    {
        return fileUploading;
    }

    /*
     * \brief setFileUploading
     * 
     * Assigns a value to the object's fileUploading variable.
     * 
     * \param u is the new boolean value being assigned to fileUploading.
    */
    public void setFileDownloading(boolean fd)
    {
        fileDownloading = fd;
    }

    /*
     * \brief getFileDownloading
     * 
     * Retreives the value currently assigned to fileDownloading.
     * 
     * Returns the boolean value of fileDownloading
    */
    public boolean getFileDownloading()
    {
        return fileDownloading;
    }
    
    /*
     * \brief deposit
     * 
     * Stores a byte array into the jagged buffer array. If a the buffer is full, any incoming thread
     * is made to wait, until notified by a withdraw operation.
     * 
     * \param data is the byte array being assigned to the buffer.
    */
    public synchronized void deposit(byte[] data)
    {
        while(fullSlots == capacity)
        {
            try
            {
                wait();
            }

            catch(InterruptedException ie)
            {

            }
        }

        buffer[in]= data;

        in = (in + 1) % capacity;

        if(fullSlots == 0)
        {
            fullSlots++;
            notify();
        }
    }

    /*
     * \brief withdraw
     * 
     * Retrieves the byte array stored in the jagged buffer array. If the buffer is empty, any incoming
     * thread is made to wait, until notified by a deposit operation.
     * 
     * Returns the byte array that has been stored in the bounded buffer.
    */
    public synchronized byte[] withdraw()
    {
        byte[] data;

        while(fullSlots == 0)
        {
            try
            {
                wait();
            }

            catch(InterruptedException ie)
            {

            }
        }

        data = buffer[out];

        out = (out + 1) % capacity;

        if(fullSlots == capacity)
        {
            fullSlots--;
            notify();
        }

        return data;
    }
}