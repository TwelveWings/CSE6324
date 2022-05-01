package cloudstorage.control;

import java.net.*;

public class BoundedBuffer 
{
    public int fullSlots = 0;
    public int capacity = 0;
    public byte[][] buffer = null;
    public int in = 0;
    public int out = 0;
    public volatile boolean fileUploaded;

    public BoundedBuffer(int c, boolean u)
    {
        capacity = c;
        fileUploaded = u;
        buffer = new byte[c][];
    }

    public void setFileUploaded(boolean u)
    {
        fileUploaded = u;
    }

    public boolean getFileUploaded()
    {
        return fileUploaded;
    }

    public synchronized void deposit(byte[] data)
    {
        while(fullSlots == capacity)
        {
            try
            {
                //System.out.println("Thread waiting at deposit");
                wait();
            }

            catch(InterruptedException ie)
            {

            }
        }

        //System.out.println("Thread depositing!");
        buffer[in]= data;

        in = (in + 1) % capacity;

        if(fullSlots == 0)
        {
            fullSlots++;
            //System.out.println("Thread notified by deposit");
            notify();
        }
    }

    public synchronized byte[] withdraw()
    {
        byte[] data;

        while(fullSlots == 0)
        {
            try
            {
                //System.out.println("Thread waiting at withdraw");
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
            //System.out.println("Thread notified by withdraw");
            notify();
        }


        //System.out.println("Thread withdrawing!");
        return data;
    }
}