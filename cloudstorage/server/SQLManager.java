package cloudstorage.server;

import cloudstorage.data.FileData;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

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
                " FileSize INT NOT NULL, " +
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

    public int deleteFile(String fileName)
    {
        ResultSet rs = selectFileByName(fileName);

        int success = -1;

        try 
        {
            if(!rs.next())
            {
                success = 0;
            }

            else
            {
                stmt = conn.createStatement();

                String sql = "DELETE FROM Files WHERE FileName = '" + fileName + "';";

                stmt.executeUpdate(sql);

                stmt.close();

                success = 1;
            }

        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return success;

    }

    public int insertData(byte[] data, int fileSize)
    {

        ResultSet rs = selectFileByName(fileName);

        int success = -1;

        String sql = "INSERT INTO Files (FileName, FileSize, Data)" + 
            " VALUES (?, ?, ?)";

        try
        {
            
            if(rs.next())
            {
                success = 0;
            }
    
            else
            {
                PreparedStatement ps = conn.prepareStatement(sql);

                ps.setString(1, fileName);
                ps.setInt(2, fileSize);
                ps.setBytes(3, data);

                ps.executeUpdate();

                success = 1;
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return success;
    }

    public List<String> selectAllFileNames()
    {
        List<String> fileNames = new ArrayList<String>();
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT FileName FROM Files;");

            while(rs.next())
            {
                fileNames.add(rs.getString("FileName"));
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return fileNames;
    }

    public ConcurrentHashMap<String, FileData> selectAllFiles()
    {
        ConcurrentHashMap<String, FileData> files = new ConcurrentHashMap<String, FileData>();
        ResultSet rs = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files;");

            while(rs.next())
            {
                FileData fd = new FileData(rs.getBytes("Data"), rs.getString("FileName"),
                    rs.getInt("FileSize"));

                files.put(rs.getString("FileName"), fd);
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return files;
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

    public void updateFileByName(String fileName, byte[] data, int fileSize)
    {
        String sql = "UPDATE Files SET Data = ?, FileSize = ? WHERE FileName = ?";

        try
        {
            PreparedStatement ps = conn.prepareStatement(sql);
            
            ps.setBytes(1, data);
            ps.setInt(2, fileSize);
            ps.setString(3, fileName);

            ps.executeUpdate();            
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

    }
}