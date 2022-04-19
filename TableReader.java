import java.sql.*;
import java.util.concurrent.*;

public class TableReader
{
    public static void main(String args[])
    {
        SQLManager sm = new SQLManager("Name.txt");

        sm.setDBConnection();

        sm.createTable();

        //sm.insertData(new byte[1], 10);

        ConcurrentHashMap<String, FileData> fd = sm.selectAllFiles();

        if(fd.size() == 0)
        {
            System.out.println("No files exist.");
        }

        else
        {
            fd.forEach((k, v) -> System.out.printf("Name: %s\nSize:%d\nData: %s", v.fileName, v.fileSize, new String(v.data, 0, v.data.length)));
        }

        sm.dropTable();
    }
}