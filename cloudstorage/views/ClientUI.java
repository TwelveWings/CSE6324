package cloudstorage.views;

import cloudstorage.control.Synchronizer;
import java.awt.event.*;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.awt.*;
import javax.swing.*;

public class ClientUI 
{
    public JFrame f;
    public JTextArea textfield1;
    public SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
    public Date date = new Date(System.currentTimeMillis());
    public String timestamp = formatter.format(date);
    public JScrollPane s1;
    public JScrollPane s2;
    public String absolutepath;
    public JFileChooser j;
    public JButton button1;
    public JButton button2;
    public JButton button3;
    public JButton button4;
    public JProgressBar progress1, progress2, progress3;
    public String iconsPath = "C:\\Users\\rydin\\OneDrive\\Documents\\GitHub\\CSE6324\\cloudstorage\\views\\icons";

    public ClientUI(Synchronizer sync) 
    {
        absolutepath = "";

        //*********************** Application Appearance Section **********************//
 
        //Creating an instance of JFrame
        f = new JFrame("Client");  
        Image icon = Toolkit.getDefaultToolkit().getImage(iconsPath + "\\icon.png");    
        f.setIconImage(icon);   

        //Progress1 - Packets Bar
        progress1 = new JProgressBar();
        progress1.setValue(0);
        progress1.setString("Waiting for Transmission");
        progress1.setStringPainted(true);
        progress1.setForeground(new Color(0,153,0));
        progress1.setBounds(170,340,195,20);
        f.add(progress1);

        //Progress2 - Blocks Bar
        progress2 = new JProgressBar();
        progress2.setValue(0);
        progress2.setStringPainted(true);
        progress2.setForeground(new Color(0,153,0));
        progress2.setBounds(170,360,195,20);
        f.add(progress2);
        
        //Button1(Dummy) - Sync Status
        ImageIcon icon1 = new ImageIcon(iconsPath + "\\in-sync.png");
        button1 = new JButton("In Sync", icon1);
        button1.setBounds(20,340,145,40);
        button1.setBackground(Color.WHITE);
        f.add(button1);        

        //Button2 - Suspend
        ImageIcon icon2 = new ImageIcon(iconsPath + "\\pause.png");
        button2 = new JButton("Suspend", icon2);
        //button2.setBounds(185,360,145,40);
        button2.setBounds(369,340,145,40);
        f.add(button2);

        //Button3 - Resume
        ImageIcon icon3 = new ImageIcon(iconsPath + "\\resume.png");
        button3 = new JButton("Resume", icon3);
        //button3.setBounds(355,360,145,40);
        button3.setBounds(519,340,145,40);
        f.add(button3);
        
        //Button4 - Delete
        ImageIcon icon4 = new ImageIcon(iconsPath + "\\delete.png");
        button4 = new JButton("Clear Log", icon4);
        button4.setBounds(519,3,145,30);
        f.add(button4);

        //Creating instance of Log Text Area
        textfield1 = new JTextArea();
        textfield1.setFont( new Font("Sans",Font.PLAIN,13));
        textfield1.setEditable(false);
        s1 = new JScrollPane(textfield1);
        s1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        s1.setBounds(20,35, 645,300);    
        f.add(s1);  
        
        //Label - Log
        JLabel label = new JLabel("Log");  
        label.setBounds(45,5, 100,30);
        label.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label);

        //*********************** Application Logic Section **********************//
        

        // Start the Pauser object to control the pause/resume functionality
        //Suspend Button Function - (Log Message)
        button2.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                sync.setIsPaused(true);
            }  
        });

        //Resume Button Function - (Log Message)
        button3.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                sync.setIsPaused(false);
                sync.resumeThread();
            }  
        });

        //Clear Button Function - (Log Message)
        button4.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                textfield1.setText("");
            }  
        });        

        //*********************** Java Swing JFrame Settings **********************//

        //Application dimensions 
        f.setSize(700,430);

        //using no layout managers 
        f.setLayout(null); 

        //making the JFrame fixed
        f.setResizable(false);

        //making the frame visible 
        f.setVisible(true); 

        //closing the GUI when stopped
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public String selectDirectory()
    {
        String absolutepath = "";
        j = new JFileChooser(new File(".\\"));
        j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = j.showOpenDialog(f);  

        if (r == JFileChooser.APPROVE_OPTION) 
        {
            String localfiles[]; 
            absolutepath = j.getSelectedFile().getAbsolutePath().replaceAll("[\\\\]", "\\\\\\\\");  
            System.out.println(absolutepath);
            File filepath = new File(absolutepath);
            return absolutepath;
        }

        else if (r == JFileChooser.CANCEL_OPTION)
        {
            System.exit(0);
        }

        return absolutepath;
    }

    public void appendToLog(String message)
    {
        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        textfield1.append(" [" + timestamp + "] " + message + "\n");
    }

    public void changeSyncStatus(String message)
    {
        if (message == "In Progress")
        {
            ImageIcon icon = new ImageIcon(iconsPath + "\\in-progress.gif");
            button1.setText("Syncing");
            button1.setIcon(icon);
            button1.setBackground(Color.orange);
        }
        else if (message == "In Sync")
        {
            ImageIcon icon = new ImageIcon(iconsPath + "\\in-sync.png");
            button1.setText("Up to date");
            button1.setIcon(icon);
            button1.setBackground(Color.white);
            progress1.setString("Transmission Successful");
            progress2.setString("");
        }
    }
}  