package cloudstorage.client;

import cloudstorage.control.Synchronizer;
import java.util.*;

public class Pauser extends Thread
{
    public Synchronizer sync;

    public Pauser(Synchronizer s)
    {
        sync = s;
    }

    public void run()
    {
        Scanner sc = new Scanner(System.in);

        while(true)
        {
            String command = sc.nextLine();

            switch(command.toLowerCase())
            {
                case "p":
                    sync.setIsPaused(true);
                    break;
                case "r":
                    sync.setIsPaused(false);
                    sync.resumeThread();
                    break;
                default:
                    System.out.println("Invalid action.");
                    break;
            }
        }
    }
}
