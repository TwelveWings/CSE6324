package cloudstorage.server;

import cloudstorage.data.FileData;
import java.util.concurrent.*;

public class TableReader
{
    public static void main(String args[])
    {
        SQLManager sm = new SQLManager("Name.txt");

        sm.setDBConnection();

        sm.createTable();

        if(args.length > 0 && args[0].equals("test"))
        {
            sm.insertData(new byte[1], 10);
        }

        ConcurrentHashMap<String, FileData> fd = sm.selectAllFiles();

        if(fd.size() == 0)
        {
            System.out.println("No files exist.");
        }

        else
        {
            byte[] a =  fd.get("bg.jpg").data;
            /*
            for(int i = 0; i < a.length; i++)
            {
                if((int)a[i] == 0)
                {
                    System.out.println(a[i]);
                }
            }*/

            fd.forEach((k, v) -> System.out.printf("Name: %s\nSize:%d\nData:\n", v.fileName, v.fileSize));
        }


        if(args.length > 0 && args[0].equals("test"))
        {
            sm.dropTable();
        }
    }
}