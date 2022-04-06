public class ServerMain
{
    public static void main(String[] args)
    {
        Server s = new Server(17, "test.txt");

        SQLManager manager = new SQLManager();

        manager.setDBConnection();

        if(args.length > 0 && args[0] == "new")
        {
            manager.dropTable();
        }

        manager.createTable();

        manager.closeConnection();

        s.startServer();
    }
}