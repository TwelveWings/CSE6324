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
        s1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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
        s2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        s2.setBounds(20,35, 320,300);     
        f.add(s2);  
        
        //Creating instance for Progress Bar 
        JProgressBar progress = new JProgressBar(0,100);
        progress.setBounds(360,300,230,30);         
        progress.setValue(89);    
        progress.setStringPainted(true);  
        progress.setForeground(new Color(0, 102, 0));   
        f.add(progress); 

        //*********************** Application Logic Section **********************//

        //Getting System timestamp for events
        SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss");
        
        //Upload Button Function - (File Chosing + Log Message)
        button2.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                File f = new File(".\\UI\\files");
                JFileChooser j = new JFileChooser(f, FileSystemView.getFileSystemView());
                Date date = new Date(System.currentTimeMillis());
                String timestamp = formatter.format(date);
                int result = j.showOpenDialog(null);
                String filename = j.getSelectedFile().getName();
                switch (result) {
                    case JOptionPane.OK_OPTION:
                        textfield1.append(" [" + timestamp + "] File Upload Started - [" + filename + "]\n");
                        break;
                    case JOptionPane.NO_OPTION:
                        textfield1.append(" [" + timestamp + "] File Upload Cancelled\n");
                        break;
                }
            }  
        });

        //Delete Button Function - (Pop up dialog panel + Log Message)
        button4.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {     
                String filename = list.getSelectedValue();
                Date date = new Date(System.currentTimeMillis());
                String timestamp = formatter.format(date);
                if(filename!=null) {                   
                    int result = JOptionPane.showConfirmDialog(null,"Are you sure about deleting " + filename + "?", "Warning!", JOptionPane.YES_NO_OPTION);
                    switch (result) {
                        case JOptionPane.YES_OPTION:
                            textfield1.append(" [" + timestamp + "] File Deleted Successfully - [" + filename + "]\n");
                            break;
                        case JOptionPane.NO_OPTION:
                            textfield1.append(" [" + timestamp + "] File Deletion Aborted\n"); 
                            break;
                    }
                }
                else {
                    textfield1.append(" [" + timestamp + "] [ERROR] No file Selected\n");
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
                Date date = new Date(System.currentTimeMillis());
                String timestamp = formatter.format(date);
                if (state == ItemEvent.SELECTED) {
                    textfield1.append(" [" + timestamp + "] File Transmission Paused\n");
                    progress.setForeground(new Color(153, 0, 0));  
                }
                else {
                    textfield1.append(" [" + timestamp + "] File Transmission Resumed\n");
                    progress.setForeground(new Color(0, 102, 0)); 
                }
            }
        };

        //Cancel Button Function - (Log Message)
        button6.addActionListener(new ActionListener()
        {  
            public void actionPerformed(ActionEvent e)
            {  
                Date date = new Date(System.currentTimeMillis());
                String timestamp = formatter.format(date);
                textfield1.append(" [" + timestamp + "] File Transmission Cancelled\n");
                progress.setValue(0);  
            }  
        });

        
        //*********************** Testing Section **********************//

        

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