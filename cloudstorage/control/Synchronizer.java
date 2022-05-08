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

    /*
     * \brief setIsPaused
     * 
     * Assigns a value to the object's isPaused variable.
     * 
     * \param p is the new boolean value being assigned to isPaused.
    */
    public void setIsPaused(boolean p)
    {
        isPaused = p;
    }

    /*
     * \brief getIsPaused
     * 
     * Retreives the value currently assigned to isPaused.
     * 
     * Returns the boolean value of isPaused
    */
    public boolean getIsPaused()
    {
        return isPaused;
    }

    /*
     * \brief checkIfPaused
     * 
     * Whenever the isPaused boolean is set to true, this will make the thread wait until notified by
     * a resume command.
    */
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

    /*
     * \brief resumeThread
     * 
     * Whenever the resume button is clicked int the UI, the isPaused boolean will be set to false and
     * this will fire notfiying the threads to resume progress.
    */
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
