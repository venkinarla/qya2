package Server;

// import everything from basic library
import Data.*;
import LocationEstimation.*;
import State.*;
//import Server.*;

import Robot.Tribot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.*;

public class UI_Server extends JPanel implements MapControl
{

	private static final long serialVersionUID = 3177827846580167915L;
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
	private JTextField clientList;
	private JTextField Inputport;
	
	private boolean connected;

	// location estimation
	private LocEstim RobotLocCalculation;
	private Vector<SignalStrength> ss = new Vector<SignalStrength>();
	private SignalVector sv = new SignalVector();
	private Vector<SignalVector> vsv = new Vector<SignalVector>();
	private UI_Protocol comProtocol;

	//*************** robot mode *******************
	private int robot_mode = 2; // 1 for data collection, 2 for exploring
	private boolean use_remote_device = true;
	
	// the robot, for controlling issue
	Tribot lego;
	//public static String log_file = "log.lgr";
	
	//*************** training data ****************
	private SignalDatabase training_data;
	
	//*************** path planning stuffs *****************
	private boolean can_move;
	private Coordinate start_state;
	private Coordinate goal_state;
	private Grid grid;
	private int gridnum = -1;
	private int counter = 0;
	
	public void setMove(boolean flag)
	{
		can_move = flag;
	}
	
	public void started()
	{
		mode_button.setEnabled(false);
		findButton.setEnabled(false);
	}	
	
	public void setMovePath( Coordinate start, Coordinate end )
	{
		start_state = start;
		goal_state = end;
		printf("Starting point: " + Coordinate.getGridNum(start) + ", Destination : " + Coordinate.getGridNum(end));
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
		
		Mapscroll = new JScrollPane(PathMap, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Mapscroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Map: ( Draggable )"));
		
		PathMap.addMouseListener(new MouseAdapter()
		{
			int ox,oy;
			public void mousePressed(MouseEvent e) 
			{
				ox = (int) e.getX();
				oy = (int) e.getY();
			}
			public void mouseReleased(MouseEvent e) 
			{
				int x = (int) e.getX();
				int y = (int) e.getY();
				int diffx = ox - x;
				int diffy = oy - y;
				int maxx = Mapscroll.getHorizontalScrollBar().getMaximum() - Mapscroll.getViewport().getWidth();
				int maxy = Mapscroll.getVerticalScrollBar().getMaximum() - Mapscroll.getViewport().getHeight();
				int newx = Mapscroll.getViewport().getViewPosition().x + diffx;
				int newy = Mapscroll.getViewport().getViewPosition().y + diffy;
				if (newx > maxx)
					newx = maxx;
				else if (newx < 0)
					newx = 0;
				if (newy > maxy)
					newy = maxy;
				else if (newy < 0)
					newy = 0;
				//System.out.println(Mapscroll.getViewport().getHeight());
				//System.out.println(Mapscroll.getVerticalScrollBar().getMaximum());
				//System.out.println(Mapscroll.getViewport().getViewPosition());
				//System.out.println(ox + " " + oy + " " + x + " " + y);
				Mapscroll.getViewport().setViewPosition(new Point(newx,newy));
			}
			public void mouseEntered( MouseEvent e )
			{
				if ((StartMon != true) &&  (EndMon != true))
				{
					Mapscroll.setCursor(new Cursor(13));
				}
				else 
					Mapscroll.setCursor(new Cursor(12));
			}
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
		
		PathMap.addMouseMotionListener(new MouseMotionListener()
		{		
			public void mouseMoved( MouseEvent e )
			{
				int x = (int) (e.getX() / ZoomFactor) ;
				int y = (int) (e.getY() / ZoomFactor) ;
				//System.out.println(e.getX() + " " + e.getY());
				//System.out.println(x + " " + y);
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
			public void mouseDragged(MouseEvent e) {
				
			}
		}
		);

		JPanel CtrDisplay = new JPanel();
		TextDisplayField = new JTextArea(0, 0);
		TextDisplayField.setEditable(false);
		TextDisplayField.setLineWrap(true);

		Textscroll = new JScrollPane(TextDisplayField, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//Textscroll.setSize(800, 200);
		Textscroll.setAutoscrolls(true);

		/*Textscroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
				{
					public void adjustmentValueChanged( AdjustmentEvent e )
					{
						Textscroll.getViewport().setViewPosition(new Point(0,Textscroll.getVerticalScrollBar().getMaximum()));
					}
				}
		);*/

		CtrDisplay.setLayout(new BorderLayout(10, 10));
		CtrDisplay.add(Textscroll, BorderLayout.CENTER);
		CtrDisplay.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Console:"));

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
		findButton.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked( MouseEvent e )
				{
					if (findButton.isEnabled())
					{
						printf("Estimating robot location, Please wait...");
						TextInputField.setText("est");
						sendMessage();
						TextInputField.setText("");
					}
				}
			}
		);		
		
		JPanel Messaging = new JPanel();
		TextInputField = new JTextField();
		Messaging.setLayout(new BorderLayout());
		Messaging.add(TextInputField, BorderLayout.CENTER);
		Messaging.add(sendButton, BorderLayout.EAST);
		CtrDisplay.add(Messaging, BorderLayout.SOUTH);

		Server_CtrPanel = new JPanel();
		LapLocPanel = new LocationPanel(this);
		clientList = new JTextField();
		clientList.setEditable(false);
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
		Server_CtrPanel.add(new JLabel("Connected Client:"), gc);
		Server_CtrPanel.add(clientList, gc);
		Server_CtrPanel.add(new JLabel(" "),gc);
		
		Server_CtrPanel.add(new JLabel("Mode Setting:"),gc);
		Server_CtrPanel.add(mode_button, gc);
		Server_CtrPanel.add(new JLabel("Estimate Location:"),gc);
		Server_CtrPanel.add(findButton,gc);	
		Server_CtrPanel.add(new JLabel(" "),gc);
		
		Server_CtrPanel.add(LapLocPanel, gc);
		
		gc0.gridwidth = GridBagConstraints.RELATIVE;
		gc0.gridheight = 2;
		gc0.weightx = 1;
		gc0.weighty = 0;
		add(Server_CtrPanel, gc0);

		gc0.gridwidth = GridBagConstraints.REMAINDER;
		gc0.gridheight = 1;
		gc0.weightx = 20;
		gc0.weighty = 3;
		add(Mapscroll, gc0);

		gc0.gridwidth = GridBagConstraints.REMAINDER;
		gc0.gridheight = 1;
		gc0.weightx = 20;
		gc0.weighty = 1;
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
						TextInputField.setText("");
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
				if (!createServer.isEnabled())
					return ;
				LapLocPanel.cleanText();
				TextDisplayField.setText(null);
				TextInputField.setText(null);
				PathMap.setSTxy(-1,-1);
				PathMap.setENDxy(-1,-1);
				//PathMap.setRobxy(-1, -1);
				PathMap.setPath(null);
				PathMap.repaint();
				if (use_remote_device)
				{
					printf("Using remote device to control the robot");
				}
				else
				{
					printf("Using local server to control the robot");
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
					printf("Opening Port: " + String.valueOf(port));
					printf("Please Wait ....");
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
		String [] input = Message.split(" ");
		if ((Message.compareToIgnoreCase("HELP") == 0) || (Message.compareToIgnoreCase("help") == 0))
		{
			printf("");
			printf("Command List");
			printf("1. HELP                                                ( Display this commnad list)");
			printf("2. MOVE [speed] [duration]                  ( Move the robot forward by the duration [MAX Speed = 720] )");
			printf("3. TURNANGLE [angle]                        ( Turn the robot by the angle [Positive angle = clockwise])");
			printf("4. GETDATA                                        ( Return the robot's sensors and motors reading");
			printf("5. GETAP                                               ( Robot will scan and return a list of signal strength records)");
			printf("6  SAVE [gird cell number]                  ( Save the scanned reading as dataset for the cell number provided)");
			printf("7. ISOBSTACLE                               ( Detect if there is obstacles around the robot )");
			//TODO
		}
		else if ((input[0].compareToIgnoreCase("CLEAR") == 0) || (input[0].compareToIgnoreCase("clear") == 0))
		{
			printf("Command : " + Message);
			ss.clear();
			vsv.clear();
			printf("Temporary signal records removed!");
		}
		else if ((input[0].compareToIgnoreCase("SAVE") == 0) || (input[0].compareToIgnoreCase("save") == 0))
		{
			printf("Command : " + Message);
			int gridnum = -1;
			try
			{
				if (input.length == 2)
				{
					gridnum = Integer.parseInt(input[1]);
					if (gridnum > 0)
					{
						vsv.add(sv);
						printf("Saving dataset for grid cell number [ " + input[1] +" ]!");
						training_data.expand(vsv, gridnum);
						training_data.saveDataSet();
						ss.clear();
						vsv.clear();
						printf("Dataset successfully saved! Temporary stored signal records removed!");
					}
					else
						printf("ERROR : Invalid grid cell number!");
				}
				else
					printf("ERROR : Invalid Argument for save command!");
			}catch (NumberFormatException e)
			{
				printf("ERROR : Invalid grid cell number!");
			}
		}
		else if (connected)
		{
			if (Message.equalsIgnoreCase("getap") && Message.equalsIgnoreCase("est"))
				printf("To Client : " + Message);
			out.println(Message);
			if (input.length == 2)
			{
				gridnum = Integer.parseInt(input[1]);
			}
		}
	}

	// override the printing function
	public void printf( String Message )
	{
		Font font = new Font("Verdana", Font.BOLD, 12);
		TextDisplayField.setFont(font);
		TextDisplayField.setForeground(Color.BLACK);
		TextDisplayField.append(Message + "\n");
		Textscroll.getViewport().setViewPosition(new Point(0,Textscroll.getVerticalScrollBar().getMaximum()));
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
		myUI.setVisible(true);
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
			printf("=======================");
			printf("A client has successfully connected!");
			printf("Client Name: " + clientSock.getInetAddress().getHostName());
			printf("Client IP: " + clientSock.getInetAddress().getHostAddress());
			printf("Client Port: " + clientSock.getPort());
			printf("=======================");
			TextDisplayField.requestFocusInWindow();
			TextInputField.requestFocusInWindow();
			
			clientList.setText(clientSock.getInetAddress().getHostName() + " : " + clientSock.getInetAddress().getHostAddress());

			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			out = new PrintWriter(clientSock.getOutputStream(), true);
			LapLocPanel.out = out;
			connected = true;
			enableALL();

			// deal with the incoming message
			String InMessage, OutMessage;
			
			// ap signal related parameters
			//boolean GetAP = false;
			//int num_sample = 1;
			//int sample_size = 1;
			//int scan_interval = 4000;
			/*if ( robot_mode == 2 )
			{
				num_sample = 1;
			}*/
			
			//Vector<Vector<SignalStrength>> RobotAPSignal = new Vector<Vector<SignalStrength>>();
			//RobotAPSignal.add(new Vector<SignalStrength>());
			//SignalVector curr_sig_vec = new SignalVector();

			// full path based on meta grid setting
			Integer[] meta_path = null;
			int meta_idx = 0; // current meta grid indicator
			Integer curr_angle = 0; // start angle of the robot, -y by default
			
			// wait for the starting signal
			while ( !can_move )
			{
					InMessage = in.readLine();
					String [] input = InMessage.split(" ");
					if ( input[0].compareToIgnoreCase("Front") == 0 ||
						 input[0].compareToIgnoreCase("ERROR") == 0 ||
						 input[0].compareToIgnoreCase("Warning") == 0
						)
					{
						printf(InMessage);
					}
					else if (input[0].compareToIgnoreCase("AP:") == 0)
					{
						if (input[1].compareToIgnoreCase("END") != 0)
						{
							//printf(input[1] + " " + input[2]);
							ss.add(new SignalStrength(input[1], "", Integer.parseInt(input[2]), -1, -1));
						}
						else
						{
							sv = new SignalVector(ss);
							printf(sv.dim + " unique AP(s) were seen! Signal records are stored TEMPORARY!");
							printf("You can rescan for more signal records or use \"SAVE [Grid cell number]\" to save the data!");
							
							if (sv.dim > 15)
							{
								vsv.add(sv);
								printf("Saving dataset for grid cell number [ " + gridnum +" ]!");
								training_data.expand(vsv, gridnum);
								training_data.saveDataSet();
								ss.clear();
								vsv.clear();
								printf("Dataset successfully saved! Temporary stored signal records removed!");
								counter++;
								// TODO
							}
							if (counter < 20)
							{
								TextInputField.setText("getap " + gridnum);
								sendMessage();
								TextInputField.setText("");
							}
							else
							{
								counter = 0;
								printf("DONE!!");
							}
						}
					}
					else if (input[0].compareToIgnoreCase("EST:") == 0)
					{
						if (input[1].compareToIgnoreCase("END") != 0)
						{
							//printf(input[1] + " " + input[2]);
							ss.add(new SignalStrength(input[1], "", Integer.parseInt(input[2]), -1, -1));
						}
						else
						{
							sv = new SignalVector(ss);
							//printf(sv.dim + " unique AP(s) were seen!");
							if (sv.dim < 10)
							{
								TextInputField.setText("est");
								sendMessage();
								TextInputField.setText("");
							}
							else
							{
								int estlocation = RobotLocCalculation.knn_estimate(sv, training_data.sig_vec_base, 5);
								PathMap.setESTxy(Coordinate.getCoord(estlocation).x, Coordinate.getCoord(estlocation).y);
								PathMap.repaint();
								printf("Estimated robot location : " + estlocation);
								ss.clear();
							}
						}
					}
					Textscroll.getViewport().setViewPosition(new Point(0,Textscroll.getVerticalScrollBar().getMaximum()));
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
						//System.out.println("start meta: " + meta_start + "\n" + "goal meta: " + meta_goal);
						meta_path = Coordinate.search(meta_start, meta_goal);
						PathMap.setPath(meta_path);
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
					/*//TODO
					// start requesting ap signal data
					RobotAPSignal = new Vector<Vector<SignalStrength>>();
					RobotAPSignal.add(new Vector<SignalStrength>());
					if ( out!= null )
						for ( int i=0; i<num_sample; ++i )
						{
							out.println("getap " + sample_size + " " + scan_interval);
						}
					*/
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
					else if ( InMessage.compareToIgnoreCase("OK") == 0)
					{
						//printf("Robot Fucking Turned!");
					}
					// while a sequence of command is finished
					else if ( InMessage.compareToIgnoreCase("FINISHED") == 0 )
					{
						can_move = true;
						//System.out.println("AT FINISHED");
					}
					
					// get all the sensors and motors readings from the device
					// and log the current state of the robot into the log file
					else if ( InMessage.length() > 5 && 
							  InMessage.substring(0, 5).equalsIgnoreCase("FRONT") ||
							  InMessage.substring(0, 5).equalsIgnoreCase("ERROR")
							)
					{
						//System.out.println("AT getdata");
						// log the state data
						//String[] data_in = InMessage.substring(7).split(",");
						/*if ( data_in.length != 6)
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
						/
						
						
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
						}*/
						int curr_meta_state = meta_path[meta_idx];
						if (curr_meta_state != Coordinate.getGridNum(goal_state))
						{
							int next_meta_state = meta_path[++meta_idx];
							// display the current position on the map
							Coordinate curr_path_state = Coordinate.getCoord(curr_meta_state);
							PathMap.setRobxy(curr_path_state.x, curr_path_state.y);
							PathMap.repaint();
							
							// execute the command
							curr_angle = stateTransCommand(curr_meta_state, next_meta_state, curr_angle, out);
							switch(curr_angle)
							{
								case 0	: 	printf("Robot current orientation: " + curr_angle + " (Facing North)");
											break;
								case 90	: 	printf("Robot current orientation: " + curr_angle + " (Facing East)");
											break;
								case 180	: 	printf("Robot current orientation: " + curr_angle + " (Facing South)");
												break;
								case -90	: 	printf("Robot current orientation: " + curr_angle + " (Facing West)");
												break;							
							}
							printf( "Robot current position: " + curr_meta_state);
							printf("==============================================");
						}
						else
						{
							// Robot reach the destination
							Coordinate Destination = Coordinate.getCoord(curr_meta_state);
							printf( "Robot current position: " + curr_meta_state);
							PathMap.setRobxy(Destination.x, Destination.y);
							PathMap.repaint();
							printf( "Robot reach the destination successfully!");
							break;
						}
					}
					
					OutMessage = comProtocol.processInput(InMessage);
					if (InMessage.compareToIgnoreCase("OK") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage + " : " + OutMessage);
					}
					else if (InMessage.compareToIgnoreCase("FINISHED") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage + " : " + OutMessage);
					}
					else if (OutMessage.compareToIgnoreCase("ERROR") == 0 || OutMessage.compareToIgnoreCase("Front") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage);
					}
					/*if (OutMessage.compareToIgnoreCase("AP") == 0)
					{
						GetAP = true;
						RobotAPSignal.lastElement().addElement(comProtocol.GetSignal());
					}
					else if (OutMessage.compareToIgnoreCase("ERROR") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage);
					}
					else if (OutMessage.compareToIgnoreCase("TURN") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + OutMessage);	
					}
					else if (OutMessage.compareToIgnoreCase("MOVE") == 0)
					{
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage + " " + OutMessage);
					}*/
					/*else if (OutMessage.compareToIgnoreCase("END") == 0)
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
					}*/
					/*else if (OutMessage.compareToIgnoreCase("HELP") == 0)
					{
						printf("Command List");
						printf("1. STOP 'DURATION'");
						printf("2. MOVE 'SPEED'	'DURATION'  (only move forward and backward)");
						printf("3. TURN 'LEFTSPEED' 'RIGHTSPEED' 'DURATION'");
						printf("4. TURNANGLE 'ANGLE' (ROBOT will stop and turn at defined angle)");
						printf("5. GETAP (Robot will scan and return signal strength)");
						printf("6. ISOBSTACLE (Detect if there is obstacle away)");
					}*/
					/*else
						printf("From client " + clientSock.getInetAddress().getHostName() + " : " + InMessage + " " + OutMessage);
						//System.out.println("OUTOUTOTUTOUTOT = " + OutMessage);*/
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
			ServerClose();
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
			clientList.setText("");
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

	/*protected void finalize()
	{
		// save the data in the end
		training_data.saveDataSet();
		ServerClose();
	}*/

	// acquire ap data from the PDA, add them to our training dataset
	/*public void getTrainingData(Vector<SignalStrength> ap_signal_list ,Coordinate curr_coord )
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
	}*/

	
	/*public int InitialAngleG()
	{
		int OrgAngle = 0;
		return OrgAngle;
	}*/
	
	// meta1 and meta2 are adjacent
	public static int stateTransCommand(Integer meta1, Integer meta2, Integer angle, PrintWriter out)
	{
		String command1 = null;				// Command to turn the robot
		String command2 = "MOVE 400 70"; 	// Command to move forward. [MOVE 450 10] about 25~30 cm. 
		
		Integer[] children = Coordinate.meta_grid_graph.get(meta1); // Get the neighbor grid number of the current grid
		
		if ( children[0] == meta2 )			// If next grid is at [EAST] of the current grid
		{
			if ( angle != 90)				// Check to see if the robot facing east
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 90);
				angle = 90;
			}
		}
		else if ( children[1] == meta2 )	// If next grid is at [SOUTH] of the current grid
		{
			if ( angle != 180 )				// Check to see if the robot facing south
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 180);
				angle = 180;
			}
		}
		else if ( children[2] == meta2 )	// If next grid is at [WEST] of the current grid
		{
			if ( angle != -90 )				// Check to see if the robot facing west
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, -90);
				angle = -90;
			}
		}
		
		else if ( children[3] == meta2 )	// If next grid is at [NORTH] of the current grid
		{
			if ( angle != 0 )				// Check to see if the robot facing north
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 0);
				angle = 0;
			}
		}
		
		String[] ret = {null, null};
		
		if ( command1 != null )
		{
			ret[0] = command1;			// turn command to turn the robot
			ret[1] = command2;			// move command to move the robot
		}
		else
		{
			ret[0] = command2;			// move command only 
		}
		
		// Send these commands out to the robot
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
