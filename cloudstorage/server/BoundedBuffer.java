package cloudstorage.server;

import java.net.*;

public class BoundedBuffer 
{
    public int fullSlots = 0;
    public int capacity = 0;
    public DatagramPacket[] buffer = null;
    public int in = 0;
    public int out = 0;

    public BoundedBuffer(int c)
    {
        capacity = c;
        buffer = new DatagramPacket[c];
    }

    public synchronized void deposit(DatagramPacket packet)
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

        buffer[in] = packet;

        in = (in + 1) % capacity;

        fullSlots++;

        if(fullSlots == 0)
        {
            notify();
        }
    }

    public synchronized DatagramPacket withdraw()
    {
        DatagramPacket packet;

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

        packet = buffer[out];

        fullSlots--;

        if(fullSlots == capacity)
        {
            notify();
        }

        return packet;
    }
}