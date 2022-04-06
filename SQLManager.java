import java.sql.*;
import javax.sql.rowset.serial.SerialBlob;

public class SQLManager
{
    public Connection conn;
    public Statement stmt;
    public String fileName;

    public SQLManager()
    {
        fileName = "";
    }

    public SQLManager(String fn)
    {
        fileName = fn;
    }

    public void setDBConnection()
    {
        try 
        {
            //Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:test.db");
        } 
        catch (Exception e) 
        {
            System.out.println(e);
        }

        System.out.println("Connection successful!");
    }

    public void createTable()
    {
        try 
        {
            stmt = conn.createStatement();
            
            String sql = "CREATE TABLE IF NOT EXISTS Files " +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                " FileName VARCHAR(30) NOT NULL," +
                " Data BLOB NOT NULL)";

            stmt.executeUpdate(sql);
            stmt.close();
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }


    public void closeConnection()
    {
        try 
        {
            conn.close();
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public void dropTable()
    {
        try 
        {
            stmt = conn.createStatement();

            String sql = "DROP TABLE Files";

            stmt.executeUpdate(sql);

            stmt.close();
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public void insertData(byte[] data)
    {
        String sql = "INSERT INTO Files " + 
            " VALUES (?, ?)";

        try
        {
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, fileName);
            ps.setBlob(2, new SerialBlob(data));

            ps.executeUpdate(sql);
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public ResultSet selectData()
    {
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files;");
        }

        catch(Exception e)
        {
            System.out.println(e);
        }

        return rs;
    }
}