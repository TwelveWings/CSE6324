package cloudstorage.control;

import javax.swing.*;

public class Synchronizer 
{
    public volatile boolean isPaused;
    public volatile boolean stopWatcher;

    public Synchronizer()
    {
        isPaused = false;
        stopWatcher = false;
    }

    public void setIsPaused(boolean p)
    {
        isPaused = p;
    }

    public boolean getIsPaused()
    {
        return isPaused;
    }

    public void checkIfDownloading()
    {
        if(stopWatcher)
        {
            JOptionPane.showMessageDialog(null, "Synchronization paused");
            synchronized(this)
            {
                while(stopWatcher)
                {
                    try
                    {
                        wait();
                    }
                    catch(InterruptedException ie)
                    {
                    }
                }
            }
        }
    }

    public void resumeThread(boolean wakeThreads)
    {
        if(wakeThreads)
        {
            synchronized(this)
            {
                notifyAll();
            }
            System.out.println("Synchronization resumed");
        }
    }

    public void setStopWatcher(boolean sw)
    {
        stopWatcher = sw;
    }

    public boolean getStopWatcher()
    {
        return stopWatcher;
    }

    public void checkIfPaused()
    {
        if(isPaused)
        {
            JOptionPane.showMessageDialog(null, "Synchronization paused");
            synchronized(this)
            {
                while(isPaused)
                {
                    try
                    {
                        wait();
                    }

                    catch(InterruptedException ie)
                    {

                    }
                }
            }
        }
    }

    public void resumeThread()
    {
        if(!isPaused)
        {
            synchronized(this)
            {
                notifyAll();
            }

            JOptionPane.showMessageDialog(null, "Synchronization resumed");
        }
    }
}
