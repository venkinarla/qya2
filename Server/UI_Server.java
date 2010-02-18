package Server;

import java.util.Calendar;

// import everything from basic library
import Data.*;
import FileIO.*;
import LocationEstimation.*;
import State.*;
//import Server.*;

import Robot.Tribot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.*;

public class UI_Server extends JPanel implements MapControl
{

	// ********************** data members ***************************	
	// main layout
	private static final double ZoomFactor = 23.3;
	private JTextArea TextDisplayField;
	private JTextField TextInputField;
	private JButton sendButton;
	private JButton findButton;
	private MapPanel PathMap;
	private LocationPanel LapLocPanel;
	private JPanel Server_CtrPanel;
	private JButton mode_button;

	// private Laptop_CtrPanel LapCtrPanel;
	private JScrollPane Mapscroll, Textscroll;
	private boolean StartMon, EndMon;

	// server-client connection
	private int port;
	private ServerSocket serverSock;
	private Socket clientSock;
	private PrintWriter out;
	private BufferedReader in;
	
	private JButton createServer;
	private JComboBox clientList;
	private JTextField Inputport;
	
	private boolean connected;

	// location estimation
	private LocEstim RobotLocCalculation;
	
	private UI_Protocol comProtocol;

	//*************** robot mode *******************
	private int robot_mode = 1; // 1 for data collection, 2 for exploring
	private boolean use_remote_device = true;
	
	// the robot, for controlling issue
	Tribot lego;
	public static String log_file = "log.lgr";
	
	//*************** training data ****************
	private SignalDatabase training_data;
	
	//*************** path planning stuffs *****************
	private boolean can_move;
	private Coordinate start_state;
	private Coordinate goal_state;
	private Grid grid;
	
	public void setMove(boolean flag)
	{
		can_move = flag;
	}
	
	public void setMovePath( Coordinate start, Coordinate stop )
	{
		start_state = start;
		goal_state = stop;
		printf("Start: " + start.toString() + ", Stop : " + stop.toString());
	}
	
	public UI_Server(Tribot robot)
	{
		// don't move
		can_move = false;
		start_state = null;
		goal_state = null;
		
		// initialize training data
		training_data = new SignalDatabase();
		training_data.loadDataSet();
		
		// load the grid map
		grid = new Grid();
		grid.loadGrid();
		
		// initialize the meta grid spec
		Coordinate.initMetaGrid();
		// initialize the meta grid graph
		Coordinate.initMetaGridGraph();
		
		// initialize the robot
		lego = robot;
		
		RobotLocCalculation = new LocEstim();
		StartMon = false;
		EndMon = false;
		
		// GUI stuffs
		setLayout(new GridBagLayout());
		GridBagConstraints gc0 = new GridBagConstraints();
		gc0.fill = GridBagConstraints.BOTH;
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		Image imgMap = new ImageIcon("src/dataset/Map.gif").getImage();
		PathMap = new MapPanel(imgMap, ZoomFactor);

		PathMap.addMouseMotionListener(new MouseMotionListener()
		{
			public void mouseMoved( MouseEvent e )
			{
				int x = (int) (e.getX() / ZoomFactor) ;
				int y = (int) (e.getY() / ZoomFactor) ;
				//System.out.println(e.getX() + " " + e.getY());
				System.out.println(x + " " + y);
				if (x >= MapPanel.MapWidth || y >= MapPanel.MapHeight)
				{
					//System.out.println(PathMap.GetMaxMapCoordx() + " OUT " + PathMap.GetMaxMapCoordy());
					return;
				}
				else if (grid.getGridMap()[y][x] != 0)
				{
					return;
				}
				else if (StartMon == true)
				{
					PathMap.setSTxy(x, y);
					PathMap.repaint();
				}
				else if (EndMon == true)
				{
					PathMap.setENDxy(x, y);
					PathMap.repaint();
				}
			}
			public void mouseDragged(MouseEvent e) 
			{
			}
		}
		);

		PathMap.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				int x = (int) (e.getX() / ZoomFactor) ;
				int y = (int) (e.getY() / ZoomFactor) ;

				if (x >= MapPanel.MapWidth || y >= MapPanel.MapHeight)
				{
					return;
				}
				else if (grid.getGridMap()[y][x] != 0)
				{
					return;
				}
				else if (StartMon == true)
				{
					LapLocPanel.setSTxy(x, y);
					StartMon = false;
					//PathMap.clearPath();
				}
				else if (EndMon == true)
				{
					LapLocPanel.setENDxy(x, y);
					EndMon = false;
					//PathMap.clearPath();
				}
			}
		});

		Mapscroll = new JScrollPane(PathMap, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Mapscroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Map:"));

		JPanel CtrDisplay = new JPanel();
		TextDisplayField = new JTextArea(0, 0);
		TextDisplayField.setEditable(false);
		TextDisplayField.setLineWrap(true);

		Textscroll = new JScrollPane(TextDisplayField, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Textscroll.setSize(800, 200);
		Textscroll.setAutoscrolls(true);

		Textscroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
				{
					public void adjustmentValueChanged( AdjustmentEvent e )
					{
						TextDisplayField.select(TextDisplayField.getHeight() + 1000, 0);
					}
				}
		);

		CtrDisplay.setLayout(new BorderLayout(10, 10));
		CtrDisplay.add(Textscroll, BorderLayout.CENTER);
		CtrDisplay.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Control Manual:"));

		sendButton = new JButton("SEND");
		sendButton.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked( MouseEvent e )
				{
					sendMessage();
				}
			}
		);
		
		findButton = new JButton("Find NXT");
		findButton.setEnabled(false);
		
		JPanel Messaging = new JPanel();
		TextInputField = new JTextField();
		Messaging.setLayout(new BorderLayout());
		Messaging.add(TextInputField, BorderLayout.CENTER);
		Messaging.add(sendButton, BorderLayout.EAST);
		CtrDisplay.add(Messaging, BorderLayout.SOUTH);

		Server_CtrPanel = new JPanel();
		LapLocPanel = new LocationPanel(this);
		clientList = new JComboBox();
		Inputport = new JTextField("560", 5);
		createServer = new JButton("Start Server!");
		Server_CtrPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = GridBagConstraints.REMAINDER;
		Server_CtrPanel.add(createServer, gc);
		
		// create the mode selection list
		mode_button = new JButton();
		mode_button.setText("Guiding Mode");
		mode_button.addActionListener(new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( robot_mode == 1 )
				{
					robot_mode = 2;
					mode_button.setText("Guiding Mode");
				}
				else
				{
					robot_mode = 1;
					mode_button.setText("Training Mode");
				}
			}
		});

		Server_CtrPanel.add(new JLabel("Server Port:"), gc);
		Server_CtrPanel.add(Inputport, gc);
		Server_CtrPanel.add(new JLabel("Client List:"), gc);
		Server_CtrPanel.add(clientList, gc);
		Server_CtrPanel.add(mode_button, gc);
		Server_CtrPanel.add(findButton,gc);		
		Server_CtrPanel.add(LapLocPanel, gc);

		gc0.gridwidth = GridBagConstraints.RELATIVE;
		gc0.weightx = 19;
		gc0.weighty = 7;
		add(Mapscroll, gc0);

		gc0.gridwidth = GridBagConstraints.REMAINDER;
		gc0.weightx = 1;
		gc0.weighty = 7;
		add(Server_CtrPanel, gc0);

		gc0.weightx = 20;
		gc0.weighty = 3;
		add(CtrDisplay, gc0);

		comProtocol = new UI_Protocol();

		TextInputField.addKeyListener(new KeyAdapter()
		{
			public void keyPressed( KeyEvent arg0 )
			{
				switch (arg0.getKeyCode())
					{
					case KeyEvent.VK_ENTER:
						sendMessage();
						break;
					default:
						;
					}
			}

			public void keyReleased( KeyEvent arg0 )
			{
			}

			public void keyTyped( KeyEvent arg0 )
			{
			}

		});

		createServer.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				if (use_remote_device)
				{
					printf("use remote device to control the robot");
				}
				else
				{
					printf("use local server to control the robot");
					lego.configure();
				}
				
				String Message = Inputport.getText();
				if (Message.length() > 0)
				{
					port = Integer.valueOf(Message).intValue();
					createServer.setEnabled(false);
					createServer.setText("Connecting...");
					Inputport.setEditable(false);

					Thread mythread = new Thread()
					{
						public void run()
						{
							CreateServer();
						}
					};
					mythread.start();
					printf("Opening Port:" + String.valueOf(port));
					printf("Please Wait....");
				}
				else
					printf("Please enter Port number");
			}
		});

		PathMap.setMap(grid.getGridMap());

		disableALL();
	}

	public void disableALL()
	{
		LapLocPanel.disableALL();
		sendButton.setEnabled(false);
		Inputport.setEditable(true);
		mode_button.setEnabled(false);
		findButton.setEnabled(false);
	}

	public void enableALL()
	{
		LapLocPanel.enableALL();
		sendButton.setEnabled(true);
		Inputport.setEditable(false);
		mode_button.setEnabled(true);
		findButton.setEnabled(true);
	}

	public void sendMessage()
	{
		String Message = TextInputField.getText();
		if (connected)
		{
			printf("To Client : " + Message);
			out.println(Message);
		}
	}

	// override the printing function
	public void printf( String Message )
	{
		TextDisplayField.append(Message + "\n");
	}

	public void OnStartMonitor()
	{
		StartMon = true;
	}

	public void OnEndMonitor()
	{
		EndMon = true;
	}

	public void OffStartMonitor()
	{
		StartMon = false;
	}

	public void OffEndMonitor()
	{
		EndMon = false;
	}

	public static void main( String[] args ) throws IOException,
			ClassNotFoundException
	{
		JFrame myUI = new JFrame();
		myUI.setTitle("Lego Robot PC Server Control Panel");
		myUI.setSize(1024, 768);
		myUI.add(new UI_Server(new Tribot()));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}


	public void CreateServer()
	{
		printf("Waiting for Client!");

		try
		{
			// listen to the socket
			serverSock = new ServerSocket(port);
			clientSock = serverSock.accept();
			TextDisplayField.requestFocusInWindow();

			printf("Client: " + clientSock.getInetAddress().getHostName());
			printf("Client: " + clientSock.getInetAddress().getHostAddress());
			printf("Client Port: " + clientSock.getPort());

			TextDisplayField.requestFocusInWindow();

			clientList.addItem(getName());

			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			out = new PrintWriter(clientSock.getOutputStream(), true);

			connected = true;
			enableALL();

			// deal with the incoming message
			String InMessage, OutMessage;
			
			// ap signal related parameters
			boolean GetAP = false;
			int num_sample = 1;
			int sample_size = 1;
			int scan_interval = 4000;
			if ( robot_mode == 2 )
			{
				num_sample = 1;
			}
			
			Vector<Vector<SignalStrength>> RobotAPSignal = new Vector<Vector<SignalStrength>>();
			RobotAPSignal.add(new Vector<SignalStrength>());
			SignalVector curr_sig_vec = new SignalVector();

			// full path based on meta grid setting
			Integer[] meta_path = null;
			int meta_idx = 0; // current meta grid indicator
			Integer curr_angle = 0; // start angle of the robot, -y by default
			
			// wait for the starting signal
			while ( !can_move )
			{
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// wait for the signal
			}
			
			while (true)
			{	
				// request a new path
				if ( meta_path == null )
				{
					if ( start_state != null && goal_state != null )
					{
						meta_idx = 0;
						int meta_start = Coordinate.getGridNum(start_state);
						int meta_goal = Coordinate.getGridNum(goal_state);
						System.out.println("start meta: " + meta_start + "\n" + 
								"goal meta: " + meta_goal);
						meta_path = Coordinate.search(meta_start, meta_goal);
						can_move = true;
					}
					else
						continue;
				}
				
				// conduct the moving stuffs
				if ( can_move )
				{	
					// ask for state information
					out.println("getdata");
					//TODO
					// start requesting ap signal data
					RobotAPSignal = new Vector<Vector<SignalStrength>>();
					RobotAPSignal.add(new Vector<SignalStrength>());
					if ( out!= null )
						for ( int i=0; i<num_sample; ++i )
						{
							out.println("getap " + sample_size + " " + scan_interval);
						}
					
					can_move = false;
				}
				
				// listen to incoming message 
				try
				{
					InMessage = in.readLine();
					
					if (InMessage == null || InMessage.equals("Bye"))
					{
						connected = false;
						break;
					}

					// while a sequence of command is finished
					if ( InMessage.compareToIgnoreCase("FINISHED") == 0 )
					{
						can_move = true;
					}
					
					// get all the sensors and motors readings from the device
					// and log the current state of the robot into the log file
					if ( InMessage.length() > 7 && 
							InMessage.substring(0, 7).equalsIgnoreCase("getdata") )
					{
						// log the state data
						String[] data_in = InMessage.substring(7).split(",");
						if ( data_in.length != 6)
						{
							System.err.println("error: data received incomplete");
						}
						else
						{
							int left_sonar_reading = Integer
									.parseInt(data_in[0].trim());
							int right_sonar_reading = Integer
									.parseInt(data_in[1].trim());
							int front_sonar_reading = Integer
									.parseInt(data_in[2].trim());
							int front_light_reading = Integer
									.parseInt(data_in[3].trim());
							int left_motor_reading = Integer
									.parseInt(data_in[4].trim());
							int right_motor_reading = Integer
									.parseInt(data_in[5].trim());

							Integer[] robot_reading = { 
									left_sonar_reading,
									right_sonar_reading, 
									front_sonar_reading,
									front_light_reading,
									left_motor_reading,
									right_motor_reading,
									curr_angle};

							Logging.logStateData(log_file, robot_reading, meta_path[meta_idx]);
						}
						
						
						
						if ( meta_idx == meta_path.length - 1 )
						{
							out.println("ISOBSTACLE");
							// last_action = null;
							meta_path = null;
							meta_idx = 0;
			
							can_move = false;
							start_state = null;
							goal_state = null;
							
							continue;
						}
					
						int curr_meta_state = meta_path[meta_idx];
						int next_meta_state = meta_path[++meta_idx];
						
						// display the current position on the map
						Coordinate curr_path_state = Coordinate.getRandCoord(curr_meta_state);
						printf( "Robot current position: " + curr_meta_state);
						PathMap.setRobxy(curr_path_state.x, 
								curr_path_state.y);
						PathMap.repaint();
						
						// execute the command
						curr_angle = stateTransCommand(
										curr_meta_state, 
										next_meta_state, 
										curr_angle,
										out);
						
						printf("Robot current orientation: " + curr_angle);
					}
					
					OutMessage = comProtocol.processInput(InMessage);
					if (OutMessage.compareToIgnoreCase("AP") == 0)
					{
						GetAP = true;
						RobotAPSignal.lastElement().addElement(comProtocol.GetSignal());
					}
					else if (OutMessage.compareToIgnoreCase("END") == 0)
					{
						GetAP = false;
						printf("finished with getting ap data, saving...");
						
						// clean up the current received data
						SignalDatabase.preProcess(RobotAPSignal.lastElement());
						
						// training data collection mode
						if ( robot_mode == 1 )
						{
							// put the current signal vector into the training set
							getTrainingData(
									new SignalVector(RobotAPSignal.lastElement()), 
									meta_path[meta_idx]);
						}
						
						// exploring mode
						if ( robot_mode == 2 )
						{
							// do nothing
						}
						
						// keep on collecting data if the number of data is not enough
						if ( RobotAPSignal.size() < num_sample )
						{
							RobotAPSignal.add(new Vector<SignalStrength>());
						}
						else
						{
							if ( robot_mode == 1 )
							{
								// save current training data
								training_data.saveDataSet();
								printf("[training mode] traning data collected");
							}
							else if ( robot_mode == 2 )
							{
								// fuse the signal vectors
								curr_sig_vec = new SignalVector(
										SignalDatabase.dataFusion(RobotAPSignal));
								printf("[exploring mode] position of the robot " + 
										meta_path[meta_idx]);
								
								// estimate the location of the robot
								Integer estim_meta_grid = 
									training_data.getLocation(curr_sig_vec);
								printf("[exploring mode] " + 7
										+ "-NN estimated location: " + estim_meta_grid);
							}

							// clear the current collected signal
							RobotAPSignal.clear();
						}
					}
					else if (OutMessage.compareToIgnoreCase("HELP") == 0)
					{
						printf("Command List");
						printf("1. STOP 'DURATION'");
						printf("2. MOVE 'SPEED'	'DURATION'  (only move forward and backward)");
						printf("3. TURN 'LEFTSPEED' 'RIGHTSPEED' 'DURATION'");
						printf("4. TURNANGLE 'ANGLE' (ROBOT will stop and turn at defined angle)");
						printf("5. GETAP (Robot will scan and return signal strength)");
						printf("6. ISOBSTACLE (Detect if there is obstacle away)");
					}
					else
						printf("From client "
								+ clientSock.getInetAddress().getHostName()
								+ " : " + InMessage);

					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}

				TextDisplayField.requestFocusInWindow();
			}

			connected = false;
			disableALL();
			printf("DisConnected!");
			createServer.setEnabled(true);
			createServer.setText("Start Server!");
		}
		catch (IOException e)
		{
			printf("Reading Error:" + e.toString());
			e.printStackTrace();

			createServer.setText("Start Server!");
			createServer.setEnabled(true);
			disableALL();
		}
		
		ServerClose();
	}

	public void ServerClose()
	{
		try
		{
			can_move = false;
			out.close();
			in.close();
			clientSock.close();
			serverSock.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected void finalize()
	{
		// save the data in the end
		training_data.saveDataSet();
		ServerClose();
	}

	// acquire ap data from the PDA, add them to our training dataset
	public void getTrainingData(Vector<SignalStrength> ap_signal_list ,Coordinate curr_coord )
	{
		for ( int i=0; i<ap_signal_list.size(); ++i )
		{
			ap_signal_list.elementAt(i).x = curr_coord.x;
			ap_signal_list.elementAt(i).y = curr_coord.y;
		}
		//System.out.println("Ap data collected: ");
		//System.out.println();
	    //training_data.signal_data.add(ap_signal_list);
	}
	
	public void getTrainingData(SignalVector novo_vec, int meta_grid)
	{
		novo_vec.meta_grid = meta_grid;
		training_data.sig_vec_base.add(novo_vec);
	}

	
	public int InitialAngleG()
	{
		int OrgAngle = 0;
		return OrgAngle;
	}
	
	// meta1 and meta2 are adjacent
	public static int stateTransCommand(
			Integer meta1, 
			Integer meta2, 
			Integer angle,
			PrintWriter out)
	{
		String command1 = null;
		String command2 = "MOVE 200 10000";
		
		Integer[] children = Coordinate.meta_grid_graph.get(meta1);
		
		if ( children[0] == meta2 )
		{
			if ( angle != 0 )
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 0);
				angle = 0;
			}
		}
		else if ( children[1] == meta2 )
		{
			if ( angle != 90 )
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 90);
				angle = 90;
			}
		}
		else if ( children[2] == meta2 )
		{
			if ( angle != 180 )
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 180);
				angle = 180;
			}
		}
		
		else if ( children[3] == meta2 )
		{
			if ( angle != -90 )
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, -90);
				angle = -90;
			}
		}
		
		String[] ret = {null, null};
		if ( command1 != null )
		{
			ret[0] = command1;
			ret[1] = command2;
			
		}
		else
		{
			ret[0] = command2;
		}
		
		// log the action
		// now this task is assigned to the robot control part
		//Logging.logActionData("log.txt", ret);
		
		// send these command out to the robot
		for ( int i=0; i<ret.length; ++i )
		{
			if ( ret[i] != null )
			{
				out.println(ret[i]);
			}
		}
		
		// return the new angle for the robot
		return angle;
	}
	
}
