import java.sql.*;

public class TableReader
{
    public static void main(String args[])
    {
        SQLManager sm = new SQLManager();

        sm.setDBConnection();

        ResultSet rs = sm.selectAllFiles();

        try
        {
            int count = 0;
            while (rs.next())
            {        
                count++;    
                int ID = rs.getInt("ID");
                String name = rs.getString("FileName");
                byte[] fileData = rs.getBytes("Data");

                System.out.println(ID);
                System.out.println(name);
                System.out.print(new String(fileData, 0, fileData.length));
            }

            if(count == 0)
            {
                System.out.println("No files exist.");
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}