package cloudstorage.data;

import cloudstorage.data.FileData;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

public class SQLManager
{
    public Connection conn;
    public Statement stmt;

    public SQLManager()
    {
        setDBConnection();
    }

    /*
     * \brief setDBConnection
     * 
     * Connects to the database
    */
    public void setDBConnection()
    {
        try 
        {
            Class.forName("org.sqlite.JDBC");

            conn = DriverManager.getConnection("jdbc:sqlite:server.db");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    /*
     * \brief createTable
     * 
     * Creates the table that will be written to by the server.
    */
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

  /*
     * \brief closeConnection
     * 
     * Closes the connection to the DB.
     * 
    */
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

  /*
     * \brief dropTable
     * 
     * Drops the table in the server. Used mainly for clean startup.
    */
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

  /*
     * \brief deleteFile
     * 
     * Deletes a file from the DB
     * 
     * \param fileName is the file being deleted.
     * 
     * Returns an int to determine if the delete was successful.
    */
    public int deleteFile(String fileName)
    {
        FileData fd = selectFileByName(fileName);

        int success = -1;

        try 
        {
            if(fd == null)
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

  /*
     * \brief insertData
     * 
     * Inserts data received from the client into the DB.
     * 
     * \param fileName is the fileName being added.
     * \param fileSize is the size of the file being added.
     * \param data is the data for the file being added.
     * 
     * Returns an int to determine if the insert was successful.
    */
    public int insertData(String fileName, int fileSize, byte[] data)
    {

        FileData fd = selectFileByName(fileName);

        int success = -1;

        String sql = "INSERT INTO Files (FileName, FileSize, Data)" + 
            " VALUES (?, ?, ?)";

        try
        {
            
            if(fd != null)
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

  /*
     * \brief selectAllFileNames
     * 
     * Gathers a list of all file names in the DB.
     * 
     * Returns a List<String> with all the file names.
    */
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

  /*
     * \brief selectAllFiles
     * 
     * Gathers a list of all files assigned to FileData in the DB.
     * 
     * Returns a ConcurrentHashMap<String, FileData> with all the files.
    */
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

  /*
     * \brief selectFileByName
     * 
     * Selects a specific file and puts it into a File object.
     * 
     * \param fileName is the name of the file being selected.
     * 
     * Returns the FileData object with the file information.
    */
    public FileData selectFileByName(String fileName)
    {
        ResultSet rs = null;
        FileData fd = null;
        try 
        {
            stmt = conn.createStatement();

            rs =  stmt.executeQuery("SELECT * FROM Files WHERE FileName = '" + fileName + "';");

            while(rs.next())
            {
                fd = new FileData(rs.getBytes("data"), rs.getString("FileName"), rs.getInt("FileSize"));
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }

        return fd;
    }

  /*
     * \brief selectFileByID
     * 
     * Selects a specific file and puts it into a ResultSet
     * 
     * \param fileID is the ID of the file being selected.
     * 
     * Returns a ResultSet for the selected file.
    */
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

  /*
     * \brief updateFileByName
     * 
     * Update a file in the database.
     * 
     * \param fileName is the name of the file being updated.
     * \param data is the new data for the file
     * \param fileSize is the new size of the data.
    */
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