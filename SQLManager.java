import java.sql.*;

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
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:test.db");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public void insertData(byte[] data)
    {

        //ResultSet rs = selectFileByName(fileName);

        String sql = "INSERT INTO Files (FileName, Data)" + 
            " VALUES (?, ?)";

        try
        {
            /*
            if(rs.next())
            {
                System.out.println("File already exists in server. Continue (1 - Yes, 2 - No)?");
                return;
            }
            */
    
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, fileName);
            ps.setBytes(2, data);

            ps.executeUpdate();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public ResultSet selectAllFiles()
    {
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files;");
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return rs;
    }


    public ResultSet selectFileByName(String fileName)
    {
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files WHERE FileName = '" + fileName + "';");
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return rs;
    }

    public ResultSet selectFileByID(String fileID)
    {
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files WHERE ID = " + fileID + ";");
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return rs;
    }
}