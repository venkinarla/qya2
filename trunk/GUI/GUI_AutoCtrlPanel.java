/**
 * A PDA Side Simulator which runs on the PC Server
 */

package GUI;

import Data.SignalStrength;
import Robot.Protocol;
import Robot.Tribot;

// the kernel
import Device.PDA;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class GUI_AutoCtrlPanel extends JPanel
{
	// network
	private int Port = 560;
	private String IP = "localhost";
	
	private boolean ServerConnected;
	
	//private boolean is_alternative = false;
	
	private JButton connectButton;
	//private JButton sendButton;
	private JTextField TextIP, TextPort;// TextInputField;
	private JTextArea TextDisplayField;
	private JScrollPane Textscroll;
	
	//private DataCollector collector;

	// the PDA control
	private PDA pda_control;

	//******************** class methods *********************
	// initializaion
	public GUI_AutoCtrlPanel(Tribot robot)
	{
		// create a new instance of the pda control system
		pda_control = new PDA(robot, "localhost", 560);
		
		// initialize the GUI
		initGUI();
		
		pda_control.IP = IP;
		pda_control.port = Port;
		//collector = new DataCollector("localhost", 520);
	}
	
	
	// initialize the GUI
	public void initGUI()
	{
		setLayout(new BorderLayout(5, 5));

		JPanel ConnectPanel = new JPanel();
		
		connectButton = new JButton("ON");
		TextIP = new JTextField("localhost", 7);
		TextPort = new JTextField("560", 5);
		
		ConnectPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.EAST;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = GridBagConstraints.LINE_START;
		//ConnectPanel.add(new JLabel("Server "));
		ConnectPanel.add(connectButton, gc);
		//ConnectPanel.add(new JLabel("scan"));
		//gc.gridwidth = GridBagConstraints.LINE_END;
		
		
		gc.gridwidth = GridBagConstraints.LINE_START;
		//ConnectPanel.add(new JLabel(" Port: "), gc);
		ConnectPanel.add(TextPort, gc);
		//ConnectPanel.add(new JLabel(" IP: "), gc);
		gc.gridwidth = GridBagConstraints.LINE_END;
		ConnectPanel.add(TextIP, gc);
		
		add(ConnectPanel, BorderLayout.NORTH);
		
		TextDisplayField = new JTextArea();
		TextDisplayField.setEditable(false);
		TextDisplayField.setSize(300, 100);
		
		Textscroll = new JScrollPane(TextDisplayField,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(Textscroll, BorderLayout.CENTER);

		/*sendButton = new JButton("ALT");
		sendButton.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( sendButton.getText() == "ALT" )
				{
					// do nothing
					System.out.println("The commnad simulation engine is not done yet, " +
							"please wait for next update");
				}
			}
		});
		
		JPanel Messaging = new JPanel();
		TextInputField = new JTextField();
		Messaging.setLayout(new BorderLayout());
		Messaging.add(TextInputField, BorderLayout.CENTER);
		Messaging.add(sendButton, BorderLayout.EAST);
		add(Messaging, BorderLayout.SOUTH);

		sendButton.setEnabled(false);*/
		ServerConnected = false;
		
		connectButton.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				Thread mythread = new Thread()
				{
					public void run()
					{
						pda_control.setIP(TextIP.getText());
						pda_control.setPort(Integer.parseInt(TextPort.getText()));
						if ( !ServerConnected )
						{
							ServerConnected = true;
							enableALL();
							pda_control.start();
						}
						/*else if((!ServerConnected) && (connectButton.getText().equalsIgnoreCase("OFF")))
						{
							ServerConnected = false;
						}*/
						else
						{		
							/*if ( ServerConnected )
							{
								pda_control.disconnectServer();
								ServerConnected = false;
								disableALL();
							}*/
						}
					}
				};
				mythread.start();
			}
		});
		
		// redirect standard out to the text area
		PrintStream red_stream = 
			new PrintStream(new JTextAreaOutputStream(TextDisplayField));
		System.setOut(red_stream);
		System.setErr(red_stream);
	}
	
	
	public void printf( String Message )
	{
		TextDisplayField.append(Message + "\n");
	}

	public void disableALL()
	{
		connectButton.setEnabled(true);
		connectButton.setText("ON");
		//sendButton.setEnabled(false);
	}

	public void enableALL()
	{
		connectButton.setEnabled(false);
		//connectButton.setText("OFF");
		//sendButton.setEnabled(true);
	}

	protected void finalize()
	{
		clientClose();
	}

	public static void main( String[] args )
	{
		JFrame myUI = new JFrame();
		myUI.setTitle("Remote Control Simulator");
		myUI.setSize(380, 520);
		myUI.add(new GUI_AutoCtrlPanel(new Tribot()));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}

	
	public void clientClose()
	{
		while( !pda_control.disconnectServer() );	
		while( !pda_control.disconnectRobot() );
	}

	// setup the romote PDA spotter
	public void CreateClient()
	{
		pda_control.start();
	}
}