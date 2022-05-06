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

    public ClientUI(Synchronizer sync) 
    {
        absolutepath = "";

        //*********************** Application Appearance Section **********************//
 
        //Creating an instance of JFrame
        f = new JFrame("Client");  
        Image icon = Toolkit.getDefaultToolkit().getImage(".\\icons\\icon.png");    
        f.setIconImage(icon);   
        
        //Creating instance of Buttons

        /*
        //Button1 - Select Directory
        ImageIcon icon1 = new ImageIcon(".\\icons\\folder.png");
        button1 = new JButton("Directory", icon1);
        button1.setBounds(20,380,145,40);
        f.add(button1);        
        */
        //Button2 - Upload
        ImageIcon icon2 = new ImageIcon(".\\icons\\pause.png");
        button2 = new JButton("Suspend", icon2);
        button2.setBounds(185,380,145,40);
        f.add(button2);

        //Button3 - Download
        ImageIcon icon3 = new ImageIcon(".\\icons\\resume.png");
        button3 = new JButton("Resume", icon3);
        button3.setBounds(355,380,145,40);
        f.add(button3);
        
        //Button4 - Delete
        ImageIcon icon4 = new ImageIcon(".\\icons\\delete.png");
        button4 = new JButton("Clear Log", icon4);
        button4.setBounds(520,380,145,40);
        f.add(button4);

        //Creating instance of Log Text Area
        textfield1 = new JTextArea();
        textfield1.setFont( new Font("Sans",Font.PLAIN,13));
        textfield1.setEditable(false);
        s1 = new JScrollPane(textfield1);
        s1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        s1.setBounds(190,35, 475,300); 
        s1.setBounds(20,35, 645,300);    
        f.add(s1);  
        
        //Creating instance of Label 
        JLabel label2;  

        //Label2 - Log
        label2 = new JLabel("Log");  
        label2.setBounds(45,5, 100,30);
        label2.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label2);

        //*********************** Application Logic Section **********************//
        
        //Append a text to notify user to select directory 
       //textfield1.append(" [-ALERT-] Select a directory to sync\n");

        /*
        //Suspend Button Function - (Log Message)
        button1.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                j = new JFileChooser(new File(".\\"));
                j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int r = j.showOpenDialog(null);  

                //Label1 - File Directory
                JLabel label1 = new JLabel("Client Files");  
                label1.setBounds(45,5, 100,30);
                label1.setFont( new Font("Sans",Font.BOLD,15));
                f.add(label1);

                if (r == JFileChooser.APPROVE_OPTION) 
                {
                    String localfiles[]; 
                    absolutepath = j.getSelectedFile().getAbsolutePath().replaceAll("[\\\\]", "\\\\\\\\");  
                    System.out.println(absolutepath);
                    File filepath = new File(absolutepath);

                    localfiles = filepath.list();
                    JList<String> list = new JList<String>(localfiles);
                    list.setEnabled(false);
                    s2 = new JScrollPane(list);
                    s2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                    s2.setBounds(20,35, 150,300);     
                    f.add(s2); 
                    f.revalidate();
                    f.repaint();
                }

                button1.setEnabled(false);
            }
        });*/

            // Start the Pauser object to control the pause/resume functionality
            //Suspend Button Function - (Log Message)
            button2.addActionListener(new ActionListener()
            {  
                public void actionPerformed(ActionEvent e)
                {  
                    Date date = new Date(System.currentTimeMillis());
                    String timestamp = formatter.format(date);
                    sync.setIsPaused(true);
                    textfield1.append(" [" + timestamp + "] File Transmission Suspended\n");
                }  
            });

            //Resume Button Function - (Log Message)
           button3.addActionListener(new ActionListener()
            {  
                public void actionPerformed(ActionEvent e)
                {  
                    Date date = new Date(System.currentTimeMillis());
                    String timestamp = formatter.format(date);
                    sync.setIsPaused(false);
                    sync.resumeThread();
                    textfield1.append(" [" + timestamp + "] File Transmission Resumed\n");
                }  
            });

        //Clear Button Function - (Log Message)
        button4.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                textfield1.setText("\n");
            }  
        });        

        //*********************** Java Swing JFrame Settings **********************//

        //Application dimensions 
        f.setSize(700,500);

        //using no layout managers 
        f.setLayout(null); 

        //making the JFrame fixed
        f.setResizable(false);

        //making the frame visible 
        f.setVisible(true); 
    }

    public String selectDirectory()
    {
        String absolutepath = "";
        j = new JFileChooser(new File(".\\"));
        j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = j.showOpenDialog(null);  

        if (r == JFileChooser.APPROVE_OPTION) 
        {
            String localfiles[]; 
            absolutepath = j.getSelectedFile().getAbsolutePath().replaceAll("[\\\\]", "\\\\\\\\");  
            System.out.println(absolutepath);
            File filepath = new File(absolutepath);
        }

        return absolutepath;
    }

    public void appendToLog(String message)
    {
        date = new Date(System.currentTimeMillis());
        timestamp = formatter.format(date);
        
        textfield1.append(" [" + timestamp + "] " + message + "\n");
    }
}  