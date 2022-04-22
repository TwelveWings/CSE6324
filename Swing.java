import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.*;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.awt.*;

public class Swing {  
    public static void main(String[] args) {  

        //*********************** Application Appearance Section **********************//
 
        //Creating an instance of JFrame
        JFrame f = new JFrame("Cloud Application");  

        //Applicaton icon
        Image icon = Toolkit.getDefaultToolkit().getImage(".\\icons\\icon.png");    
        f.setIconImage(icon);   
        
        //Creating instance of Buttons  
        JButton button1, button2, button3, button4, button6;

        //Button1 - Edit
        ImageIcon icon1 = new ImageIcon(".\\icons\\edit.png");
        button1 = new JButton("Edit", icon1);
        button1.setBounds(20,380,145,40);
        f.add(button1);

        //Button2 - Upload
        ImageIcon icon2 = new ImageIcon(".\\icons\\upload.png");
        button2 = new JButton("Upload", icon2);
        button2.setBounds(185,380,145,40);
        f.add(button2);

        //Button3 - Download
        ImageIcon icon3 = new ImageIcon(".\\icons\\download.png");
        button3 = new JButton("Download", icon3);
        button3.setBounds(355,380,145,40);
        f.add(button3);
        
        //Button4 - Delete
        ImageIcon icon4 = new ImageIcon(".\\icons\\delete.png");
        button4 = new JButton("Delete", icon4);
        button4.setBounds(520,380,145,40);
        f.add(button4);

        //Button5 - Pause/Resume
        // button5 = new JButton(new ImageIcon(".\\icons\\resume.png"));
        // button5.setBounds(600,300,30,30);
        // f.add(button5);

        //Creating Instance of Pause/Resume Toggle
        JToggleButton toggle1 = new JToggleButton(new ImageIcon(".\\icons\\resume.png"));
        toggle1.setBounds(600,300,30,30);
        f.add(toggle1);

        //Button6 - Cancel 
        button6 = new JButton(new ImageIcon(".\\icons\\cancel.png"));
        button6.setBounds(635,300,30,30);
        f.add(button6);

        //Creating instance for Scroll Panel
        JScrollPane s1, s2;

        //Creating instance of Log Text Area
        JTextArea textfield1 = new JTextArea();
        textfield1.setFont( new Font("Sans",Font.PLAIN,10));
        textfield1.setEditable(false);
        s1 = new JScrollPane(textfield1);
        s1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        s1.setBounds(360,35, 305,230);     
        f.add(s1);  
        
        //Creating instance of Label 
        JLabel label1, label2, label3, label4;  

        //Label1 - File Directory
        label1 = new JLabel("File Directory");  
        label1.setBounds(130,5, 100,30);
        label1.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label1);

        //Label2 - Log
        label2 = new JLabel("Log");  
        label2.setBounds(500,5, 100,30);
        label2.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label2);

        //Label3 - File Progress
        label3 = new JLabel("File Progress");  
        label3.setBounds(430,270, 100,30);
        label3.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label3);

        //Label4 - File Actions
        label4 = new JLabel("File Actions");  
        label4.setBounds(310,340, 100,30);
        label4.setFont( new Font("Sans",Font.BOLD,15));
        f.add(label4);

        //Creating instance for Scrollable List
        String localfiles[];
        File filepath = new File(".\\files\\");
        localfiles = filepath.list();
        JList<String> list = new JList<String>(localfiles);
        s2 = new JScrollPane(list);
        s2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        s2.setBounds(20,35, 320,300);     
        f.add(s2);  
        
        //Creating instance for Progress Bar 
        JProgressBar progress = new JProgressBar(0,100);
        progress.setBounds(360,300,230,30);         
        progress.setValue(89);    
        progress.setStringPainted(true);    
        f.add(progress); 

        //*********************** Application Logic Section **********************//

        //Getting System timestamp for events
        SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String timestamp = formatter.format(date);
        
        //Upload Button Function - (File Chosing + Log Message)
        button2.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                File f = new File(".\\UI\\files");
                JFileChooser j = new JFileChooser(f, FileSystemView.getFileSystemView());
                j.showOpenDialog(null);
                textfield1.append(" [" + timestamp + "] File Upload Started\n");
            }  
        });

        //Delete Button Function - (Pop up dialog panel + Log Message)
        button4.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {     
                int result = JOptionPane.showConfirmDialog(null, "Are you sure about deleting this file?");
                switch (result) {
                    case JOptionPane.YES_OPTION:
                    textfield1.append(" [" + timestamp + "] File Deleted Successfully\n");
                    break;
                    case JOptionPane.NO_OPTION:
                    textfield1.append(" [" + timestamp + "] File Deletion Aborted\n"); 
                    break;
                    case JOptionPane.CANCEL_OPTION:
                    textfield1.append(" [" + timestamp + "] File Deletion Cancelled\n"); 
                    break;
                }
            }  
        });

        //Pause/Resume Button Function - (Log Message)
        ItemListener itemListener = new ItemListener() {
 
            //itemStateChanged() method is invoked automatically
            public void itemStateChanged(ItemEvent itemEvent)
            {
                //event is generated in a button
                int state = itemEvent.getStateChange();

                if (state == ItemEvent.SELECTED) {
                    textfield1.append(" [" + timestamp + "] File Transmission Paused\n");
                }
                else {
                    textfield1.append(" [" + timestamp + "] File Transmission Resumed\n");
                }
            }
        };

        //Cancel Button Function - (Log Message)
        button6.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                textfield1.append(" [" + timestamp + "] File Transmission Cancelled\n");
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

        //Listener for Toggle1
        toggle1.addItemListener(itemListener);
    }  
}  