package cloudstorage.control;

import java.util.*;
import javax.swing.*;

public class Synchronizer 
{
    public volatile boolean isPaused;
    public HashMap<String, Boolean> blockedFiles;

    public Synchronizer()
    {
        isPaused = false;
        blockedFiles = new HashMap<String, Boolean>();
    }

    public void setIsPaused(boolean p)
    {
        isPaused = p;
    }

    public boolean getIsPaused()
    {
        return isPaused;
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
