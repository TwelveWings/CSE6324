package cloudstorage.views;

import javax.swing.*;
import java.awt.event.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.awt.*;

public class ServerUI 
{
    public JFrame f;
    public JTextArea textfield1;
    public SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public Date date = new Date(System.currentTimeMillis());
    public String timestamp = formatter.format(date);
    public String iconsPath;

    public ServerUI() 
    {
        //*********************** Server GUI Appearance Section **********************//
 
        iconsPath = System.getProperty("user.dir") + "/cloudstorage/views/icons";
        //Creating an instance of JFrame
        f = new JFrame("Server");  
        Image icon = Toolkit.getDefaultToolkit().getImage(iconsPath + "\\icon.png");    
        f.setIconImage(icon);   
        
        //Button1 - To Clear Log file
        ImageIcon icon1 = new ImageIcon(iconsPath + "\\delete.png");
        JButton button1 = new JButton("Clear Log", icon1);
        button1.setBounds(519,3,145,30);
        f.add(button1);
        
        //Creating instance of Log Text Area
        textfield1 = new JTextArea();
        textfield1.setFont( new Font("Sans",Font.PLAIN,13));
        textfield1.setEditable(false);
        JScrollPane s1 = new JScrollPane(textfield1);
        s1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        s1.setBounds(20,35, 645,400);
        f.add(s1);
         
        //Label1 - Log
        JLabel label = new JLabel("Log");  
        label.setBounds(45,5, 100,30);
        label.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label);

        //*********************** Application Logic Section **********************//

        //Clear Button Function - (Log Message)
        button1.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                textfield1.setText("");
            }  
        });
        
        //*********************** Java Swing JFrame Settings **********************//

        //Application dimensions 
        f.setSize(700,490);

        //using no layout managers 
        f.setLayout(null); 

        //making the JFrame fixed
        f.setResizable(false);

        //making the frame visible 
        f.setVisible(true); 

        //closing the GUI when stopped
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //*********************** Server Actual Logic Section **********************//
    }

    /*
     * \brief appendToLog
     * 
     * Writes a message to the UI log
     * 
     * \param message is the message being written.
    */
    public void appendToLog(String message)
    {
        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        
        textfield1.append(" [" + timestamp + "] " + message + "\n");
    }
}  