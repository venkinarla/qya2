package Server;

import Data.*;
import LocationEstimation.*;
import State.*; //import Server.*;

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
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.*;

public class UI_Server extends JPanel implements MapControl {

	private static final long serialVersionUID = 3177827846580167915L;
	private static final double ZoomFactor = 23.3;		// Basically means the width of each cell
	private JTextArea TextDisplayField;					// Console at the bottom
	private JTextField TextInputField;					// Input field at the bottom
	private JButton sendButton;							// The send button for command
	private JButton findButton;							// The find NXT button
	private MapPanel PathMap;							// The map
	private LocationPanel LapLocPanel;					// The lower part of the control panel
	private JPanel Server_CtrPanel;						// The upper part of the control panel
	private JScrollPane Mapscroll, Textscroll;			// The scrolls that hold the map and the console
	private boolean StartMon, EndMon;					// The boolean value that tell if the set start and destination button is selected
	
	private int port;									// The server side connection port value
	private ServerSocket serverSock;					// The server side socket
	private Socket clientSock;							// The client side socket
	private PrintWriter out;							// The client side output stream
	private BufferedReader in;							// The server side input stream
	private JButton createServer;						// The start server button
	private JButton mode_button;						// The mode selection button
	private JTextField clientList;						// The text field that display the client information
	private JTextField Inputport;						// The text field for connection port
	private boolean connected;							// The boolean value that tell if a client is connected to the server

	// location estimation
	private LocEstim RobotLocCalculation;								// The object that used to call the localization function
	private Vector<SignalStrength> ss = new Vector<SignalStrength>();	// A vector of signal strength data used to store the data temporary.
	private SignalVector sv = new SignalVector();						// A signal vector used to count the unique access point received
	private Vector<SignalVector> vsv = new Vector<SignalVector>();		// A vector of signal vector used to save the data
	private UI_Protocol comProtocol;					// The protocol used process the input command
	private Boolean[] blocked;							// A array of boolean value that tell if a cell is blocked
	private int robot_mode = 2; 						// Mode of the robot. 1 for auto data collection, 2 for self-guiding.
	Tribot lego;										// The object to control the robot

	// training data
	private SignalDatabase training_data;				// The database used for localization
	private boolean can_move;							// The boolean value that tell if the robot can start self-guiding
	private Coordinate start_state;						// The start location of the path
	private Coordinate goal_state;						// The destination of the path
	private Grid grid;									// The grid map loaded

	/**
	 * Tell the server to start self-guiding
	 * 
	 * @param flag the boolean value
	 */
	public void setMove(boolean flag) 
	{
		can_move = flag;
	}

	/**
	 * Start self-guiding
	 * 
	 */
	public void started() 
	{
		mode_button.setEnabled(false);
		findButton.setEnabled(false);
	}

	/**
	 * Setup the starting point and the destination for self-guiding
	 * 
	 * @param start the Coordinate of starting point
	 * @param end the Coordinate of destination
	 */
	public void setMovePath(Coordinate start, Coordinate end) 
	{
		start_state = start;
		goal_state = end;
		printf("Starting point: " + Coordinate.getGridNum(start) + ", Destination : " + Coordinate.getGridNum(end));
	}

	/**
	 * Instantiates a new Server UI.
	 * 
	 * @param robot the lego robot
	 */
	public UI_Server(Tribot robot) 
	{
		can_move = false;				
		start_state = null;
		goal_state = null;
		StartMon = false;
		EndMon = false;
		
		// Initialize training data
		training_data = new SignalDatabase();
		training_data.loadDataSet();

		// Load the grid map
		grid = new Grid();
		grid.loadGrid();

		// Initialize the meta grid spec
		Coordinate.initMetaGrid();
		// Initialize the meta grid graph
		Coordinate.initMetaGridGraph();

		// Initialize the robot
		lego = robot;

		// Initialize the localization function
		RobotLocCalculation = new LocEstim();

		// GUI
		setLayout(new GridBagLayout());										
		GridBagConstraints gc0 = new GridBagConstraints();
		gc0.fill = GridBagConstraints.BOTH;
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		Image imgMap = new ImageIcon("src/dataset/Map.gif").getImage();			// Load the map file
		PathMap = new MapPanel(imgMap, ZoomFactor);								// Create the MapPanel object

		Mapscroll = new JScrollPane(PathMap,									// Place the MapPanel object inside a scroll panel
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Mapscroll.setBorder(BorderFactory.createTitledBorder(BorderFactory		// Setup the border and the name of the MapPanel
				.createLoweredBevelBorder(), "Map: ( Draggable )"));

		PathMap.addMouseListener(new MouseAdapter() 
		{
			int ox, oy;									// The x,y position when the mouse is pressed

			public void mousePressed(MouseEvent e) 
			{
				ox = (int) e.getX();					// Get the x,y position when the mouse is pressed
				oy = (int) e.getY();
			}

			public void mouseReleased(MouseEvent e) 	// Make the Map draggable
			{
				int x = (int) e.getX();					// Get the x,y position when the mouse is released
				int y = (int) e.getY();
				int diffx = ox - x;						// Find the differences between two position
				int diffy = oy - y;
				int maxx = Mapscroll.getHorizontalScrollBar().getMaximum()			// Find the maximum possible x,y value for the map
						- Mapscroll.getViewport().getWidth();
				int maxy = Mapscroll.getVerticalScrollBar().getMaximum()
						- Mapscroll.getViewport().getHeight();
				int newx = Mapscroll.getViewport().getViewPosition().x + diffx;		// Fine the new x,y value by adding the differences
				int newy = Mapscroll.getViewport().getViewPosition().y + diffy;		
				if (newx > maxx)													// Use the maximum or zero ifthe new value is larger than the limit
					newx = maxx;
				else if (newx < 0)
					newx = 0;
				if (newy > maxy)
					newy = maxy;
				else if (newy < 0)
					newy = 0;
				Mapscroll.getViewport().setViewPosition(new Point(newx, newy));		// Update the Map
			}

			public void mouseEntered(MouseEvent e)	 // Change the mouse cursor to cross
			{
				if ((StartMon != true) && (EndMon != true)) 
				{
					Mapscroll.setCursor(new Cursor(13));
				} 
				else
				{
					Mapscroll.setCursor(new Cursor(12));
				}
			}

			public void mouseClicked(MouseEvent e)	// Setup the starting location and destination
			{
				int x = (int) (e.getX() / ZoomFactor);					// The x,y coordinate when the mouse is clicked
				int y = (int) (e.getY() / ZoomFactor);

				if (x >= MapPanel.MapWidth || y >= MapPanel.MapHeight) 
				{
					return;												// Ignore if it is outside the limit
				} 
				else if (grid.getGridMap()[y][x] != 0) 
				{
					return;												// Ignore if it is a unmoveable place
				} 
				else if (StartMon == true) 								
				{
					LapLocPanel.setSTxy(x, y);							// Setup the starting location
					StartMon = false;
					// PathMap.clearPath();
				} 
				else if (EndMon == true) 
				{
					LapLocPanel.setENDxy(x, y);							// Setup the destination
					EndMon = false;
					// PathMap.clearPath();
				}
			}
		});

		PathMap.addMouseMotionListener(new MouseMotionListener() 
		{
			public void mouseMoved(MouseEvent e) 						// Update the position of the colored dot 
			{
				int x = (int) (e.getX() / ZoomFactor);					// The x,y coordinate when the mouse is pointed to
				int y = (int) (e.getY() / ZoomFactor);
				if (x >= MapPanel.MapWidth || y >= MapPanel.MapHeight) 
				{
					return;												// Ignore if it is outside the limit
				} 
				else if (grid.getGridMap()[y][x] != 0) 
				{
					return;												// Ignore if it is a unmoveable place
				} 
				else if (StartMon == true) 
				{
					PathMap.setSTxy(x, y);								// Update the location of the starting location dot
					PathMap.repaint();
				} 
				else if (EndMon == true) 
				{
					PathMap.setENDxy(x, y);								// Update the location of the destination dot
					PathMap.repaint();
				}
			}
			public void mouseDragged(MouseEvent e) 						// Useless currently
			{
			}
		});

		JPanel CtrDisplay = new JPanel();								// Create the console panel
		TextDisplayField = new JTextArea(0, 0);							// Create the TextDisplayField object
		TextDisplayField.setEditable(false);
		TextDisplayField.setLineWrap(true);

		Textscroll = new JScrollPane(TextDisplayField,					// Place the TextDisplayField object inside a scroll panel
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Textscroll.setAutoscrolls(true);

		CtrDisplay.setLayout(new BorderLayout(10, 10));
		CtrDisplay.add(Textscroll, BorderLayout.CENTER);				// Add the TextDisplayField in the console panel
		CtrDisplay.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createLoweredBevelBorder(), "Console:"));

		sendButton = new JButton("SEND");								// Create the send button
		sendButton.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent e) 
			{
				sendMessage();
			}
		});

		findButton = new JButton("Find NXT");							// Create the Find NX button
		findButton.setEnabled(false);
		findButton.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent e) 						// Start the localization function
			{
				if (findButton.isEnabled()) 
				{
					printf("Estimating robot location, Please wait...");
					TextInputField.setText("est");	
					ss.clear();
					sendMessage();
					TextInputField.setText("");
				}
			}
		});

		JPanel Messaging = new JPanel();								// Create a panel holding the TextInputField and sendButton
		TextInputField = new JTextField();
		Messaging.setLayout(new BorderLayout());
		Messaging.add(TextInputField, BorderLayout.CENTER);
		Messaging.add(sendButton, BorderLayout.EAST);
		CtrDisplay.add(Messaging, BorderLayout.SOUTH);					// Add every thing into the console panel

		Server_CtrPanel = new JPanel();									// Create a control panel
		LapLocPanel = new LocationPanel(this);							// Create a Location panel
		clientList = new JTextField();
		clientList.setEditable(false);
		Inputport = new JTextField("560", 5);
		createServer = new JButton("Start Server!");
		Server_CtrPanel.setLayout(new GridBagLayout());					
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = GridBagConstraints.REMAINDER;
		Server_CtrPanel.add(createServer, gc);							
		
		mode_button = new JButton();									// Create the mode selection button
		mode_button.setText("Guiding Mode");
		robot_mode = 2;
		mode_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (robot_mode == 1) {
					robot_mode = 2;
					mode_button.setText("Guiding Mode");
				} else {
					robot_mode = 1;
					mode_button.setText("Training Mode");
				}
			}
		});

		Server_CtrPanel.add(new JLabel("Server Port:"), gc);
		Server_CtrPanel.add(Inputport, gc);
		Server_CtrPanel.add(new JLabel("Connected Client:"), gc);
		Server_CtrPanel.add(clientList, gc);
		Server_CtrPanel.add(new JLabel(" "), gc);

		Server_CtrPanel.add(new JLabel("Mode Setting:"), gc);
		Server_CtrPanel.add(mode_button, gc);
		Server_CtrPanel.add(new JLabel("Estimate Location:"), gc);
		Server_CtrPanel.add(findButton, gc);
		Server_CtrPanel.add(new JLabel(" "), gc);

		Server_CtrPanel.add(LapLocPanel, gc);
		Server_CtrPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Control Panel"));
		
		// Add everything to the server UI
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
			public void keyPressed(KeyEvent arg0) 		// Setup the enter key for sending command
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
			public void keyReleased(KeyEvent arg0) 
			{
			}
			public void keyTyped(KeyEvent arg0) 
			{
			}

		});

		createServer.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent e) 			// user click the start server button
			{
				if (!createServer.isEnabled())
					return;
				LapLocPanel.cleanText();					// Remove the value of the last time
				TextDisplayField.setText(null);
				TextInputField.setText(null);
				PathMap.setSTxy(-1, -1);
				PathMap.setENDxy(-1, -1);
				PathMap.setPath(null);
				PathMap.repaint();

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
							CreateServer();					// Create a thread for the server to run
						}
					};
					mythread.start();
					printf("Opening Port: " + String.valueOf(port));
					printf("Please Wait ....");
				} else
					printf("Please enter Port number");
			}
		});

		PathMap.setMap(grid.getGridMap());
		blocked = new Boolean[Coordinate.meta_grid.size()];		// Setup the boolean value of blockage 
		for (int i = 0 ; i < Coordinate.meta_grid.size() ; i++)
		{
			blocked[i] = false;
		}
		//blocked[2] = true;
		disableALL();											// disable all button before connection
	}

	/**
	 * Disable all buttons
	 */
	public void disableALL() {
		LapLocPanel.disableALL();
		sendButton.setEnabled(false);
		Inputport.setEditable(true);
		mode_button.setEnabled(false);
		findButton.setEnabled(false);
	}

	/**
	 * Enable all buttons
	 */
	public void enableALL() {
		LapLocPanel.enableALL();
		sendButton.setEnabled(true);
		Inputport.setEditable(false);
		mode_button.setEnabled(true);
		findButton.setEnabled(true);
	}

	/**
	 * Handle the command from the user
	 */
	public void sendMessage() {
		String Message = TextInputField.getText();
		String[] input = Message.split(" ");
		// "HELP"
		if ((Message.compareToIgnoreCase("HELP") == 0) || (Message.compareToIgnoreCase("help") == 0)) 
		{
			printf("");
			printf("Command List");
			printf("1. HELP                                                ( Display this commnad list)" );
			printf("2. MOVE [speed] [duration]                  ( Move the robot forward by the duration [MAX Speed = 720] )");
			printf("3. TURNANGLE [angle]                        ( Turn the robot by the angle [Positive angle = clockwise] )");
			printf("4. GETDATA                                        ( Return the robot's sensors and motors reading )");
			printf("5. GETAP                                               ( Robot will scan and return a list of signal strength records )");
			printf("6  SAVE [cell number]                  ( Save the scanned reading as dataset for the cell number provided )");
			printf("7. ISOBSTACLE                               ( Detect if there is obstacles around the robot )");
		} 
		// "CLEAR"
		else if ((input[0].compareToIgnoreCase("CLEAR") == 0) || (input[0].compareToIgnoreCase("clear") == 0)) 
		{
			printf("Command : " + Message);
			ss.clear();
			vsv.clear();
			printf("Temporary signal records removed!");
		} 
		// "SAVE [gridnum]"
		else if ((input[0].compareToIgnoreCase("SAVE") == 0) || (input[0].compareToIgnoreCase("save") == 0)) 
		{
			printf("Command : " + Message);
			int gridnum = -1;
			try {
				if (input.length == 2) 
				{
					gridnum = Integer.parseInt(input[1]);
					if (gridnum > 0) 
					{
						vsv.add(sv);
						printf("Saving dataset for grid cell number [ " + input[1] + " ]!");
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
			} 
			catch (NumberFormatException e) 
			{
				printf("ERROR : Invalid grid cell number!");
			}
		} 
		// For other command, send to the client
		else if (connected) 
		{			
			if (!Message.equalsIgnoreCase("getap") && !Message.equalsIgnoreCase("est"))
				printf("To Client : " + Message);
			out.println(Message);
		}
	}

	/**
	 * Override the printing function
	 * 
	 * @param Message the string displayed
	 */
	public void printf(String Message) 
	{
		Font font = new Font("Verdana", Font.BOLD, 12);
		TextDisplayField.setFont(font);
		TextDisplayField.setForeground(Color.BLACK);
		TextDisplayField.append(Message + "\n");
		Textscroll.getViewport().setViewPosition(
				new Point(0, Textscroll.getVerticalScrollBar().getMaximum()));
	}

	/**
	 * Handle the starting location button clicked
	 */
	public void OnStartMonitor() 
	{
		StartMon = true;
	}

	/**
	 * Handle the destination button clicked
	 */
	public void OnEndMonitor() 
	{
		EndMon = true;
	}

	/**
	 * Handle the starting location button released
	 */
	public void OffStartMonitor() 
	{
		StartMon = false;
	}

	/**
	 * Handle the destination button released
	 */
	public void OffEndMonitor() 
	{
		EndMon = false;
	}

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException 
	{
		JFrame myUI = new JFrame();
		myUI.setTitle("Lego Robot PC Server Control Panel");
		myUI.setSize(1024, 740);
		myUI.add(new UI_Server(new Tribot()));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.setVisible(true);
	}

	/**
	 * The function called when user start the server.
	 * Basically, the whole GUI start in here.
	 */
	public void CreateServer() 
	{
		printf("Waiting for Client!");

		try {
			// listen to the socket
			serverSock = new ServerSocket(port);
			clientSock = serverSock.accept();
			TextDisplayField.requestFocusInWindow();
			printf("=======================");										// Display the client information
			printf("A client has successfully connected!");
			printf("Client Name: " + clientSock.getInetAddress().getHostName());
			printf("Client IP: " + clientSock.getInetAddress().getHostAddress());
			printf("Client Port: " + clientSock.getPort());
			printf("=======================");
			TextDisplayField.requestFocusInWindow();
			TextInputField.requestFocusInWindow();

			clientList.setText(clientSock.getInetAddress().getHostName()				// Update the text field
					+ " : " + clientSock.getInetAddress().getHostAddress());

			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream())); // Setup the input, output stream
			out = new PrintWriter(clientSock.getOutputStream(), true);
			LapLocPanel.out = out;
			connected = true;
			enableALL();

			String InMessage, OutMessage;						// The input and output message

			Integer[] meta_path = null;
			int meta_idx = 0; 									// current meta grid indicator
			Integer curr_angle = 0; 							// start angle of the robot, 0 by default
			int iter = 0;										// iterator for auto data collection
			boolean collected = true;
			
			// The server response to input before self-guiding function start
			while (!can_move) 
			{
				InMessage = in.readLine();
				String[] input = InMessage.split(" ");
				// "GETDATA"
				if (input[0].compareToIgnoreCase("Front") == 0 || input[0].compareToIgnoreCase("ERROR") == 0 || input[0].compareToIgnoreCase("Warning") == 0) 
				{
					printf(InMessage);
				} 
				// "GETAP"
				else if (input[0].compareToIgnoreCase("AP:") == 0) 
				{
					if (input[1].compareToIgnoreCase("END") != 0) 
					{
						printf(input[1] + " " + input[2]);
						ss.add(new SignalStrength(input[1], "", Integer.parseInt(input[2]), -1, -1)); // Store the signal strength in ss
					} 
					else 
					{
						sv = new SignalVector(ss);
						vsv.add(sv);
						printf(sv.dim + " unique AP(s) were seen! Signal records are stored TEMPORARY!");
						printf("You can rescan for more signal records or use \"SAVE [Grid cell number]\" to save the data!");
						printf("==============================================");
					}
				}
				// "EST" or Click "Find NXT"
				else if (input[0].compareToIgnoreCase("EST:") == 0)
				{
					if (input[1].compareToIgnoreCase("END") != 0)
					{
						ss.add(new SignalStrength(input[1], "", Integer.parseInt(input[2]), -1, -1));
					}
					else
					{
						sv = new SignalVector(ss);
						printf(sv.dim + " unique AP(s) were seen!");
						int estlocation = RobotLocCalculation.bayesianEstimation(sv, training_data.sig_vec_base_v);		// Use the bayesian estimation
						PathMap.setESTxy(Coordinate.getCoord(estlocation).x, Coordinate.getCoord(estlocation).y);		// Display the estimated location
						PathMap.repaint();
						printf("Estimated robot location : " + estlocation);
						printf("==============================================");
						ss.clear();
					}
				}
				Textscroll.getViewport().setViewPosition(new Point(0, Textscroll.getVerticalScrollBar().getMaximum()));
			}
			
			ArrayList<Integer> collectpath = new ArrayList<Integer>();
			while (true) 
			{
				if (meta_path == null) 								// Request a new guiding path
				{
					if (start_state != null && goal_state != null) 
					{
						if (robot_mode == 2)						// For self-guiding
						{
							meta_idx = 0;
							int meta_start = Coordinate.getGridNum(start_state);
							int meta_goal = Coordinate.getGridNum(goal_state);
							meta_path = Coordinate.search(meta_start, meta_goal, blocked); // Use BFS to generate the path
							PathMap.setPath(meta_path);
							can_move = true;
						}
						else										// For auto data collection
						{
							meta_idx = 0;
							int meta_start = Coordinate.getGridNum(start_state);
							int meta_goal = Coordinate.getGridNum(goal_state);
							Integer [] temp2 = null;	
							collectpath = new ArrayList<Integer>();
							ArrayList<Integer[]> allpath = new ArrayList<Integer[]>();
							if (meta_goal > meta_start)
							{
								for (int i = meta_start; i < meta_goal; i++)
								{
									collectpath.add(i);
									allpath.add(Coordinate.search(i, i+1, blocked));	// Use BFS to generate the path between each grid
								}
							}
							else
							{
								for (int i = meta_start; i > meta_goal; i--)
								{
									collectpath.add(i);
									allpath.add(Coordinate.search(i, i-1, blocked));
								}
							}
							int totallength = 0;
							for (int i = 0; i < allpath.size(); i++)
							{
								totallength += allpath.get(i).length;
							}
							totallength = totallength - (allpath.size() - 1);
							int counter = allpath.get(0).length;
							temp2 = new Integer[totallength];
							for (int i = 0; i<allpath.get(0).length;i++)
							{
								temp2[i] = allpath.get(0)[i];
							}
							for (int i = 1; i<allpath.size();i++)
							{
								for (int j = 1 ; j <allpath.get(i).length ; j++)
								{
									temp2[counter] = allpath.get(i)[j];				// Build up the whole path
									counter++;
								}
							}
							meta_path = temp2;
							PathMap.setPath(meta_path);
							can_move = true;
						}
					} else
						continue;
				}

				if (can_move) 								// if path for self-guiding or auto data collection has generated
				{
					out.println("temp");					// send a useless command to start guiding (Can change to "GETDATA")
					can_move = false;
				}

				// The server response to incoming message during self-guiding or auto data collection.
				try 
				{
					InMessage = in.readLine();
					String[] input = InMessage.split(" ");
					
					// "BYE" or null
					if (InMessage == null || InMessage.equals("Bye")) 
					{
						connected = false;
						break;
					} 
					// "OK" turn finished
					else if (InMessage.compareToIgnoreCase("OK") == 0) 
					{
					}
					// "FINISHED" while a sequence of command is finished
					else if (InMessage.compareToIgnoreCase("FINISHED") == 0) 
					{
						can_move = true;
					}
					// "EST" at the end of the journey
					else if (input[0].compareToIgnoreCase("EST:") == 0)
					{
						if (input[1].compareToIgnoreCase("END") != 0)
						{
							ss.add(new SignalStrength(input[1], "", Integer.parseInt(input[2]), -1, -1));
						}
						else
						{
							sv = new SignalVector(ss);
							printf(sv.dim + " unique AP(s) were seen!");
							int estlocation = RobotLocCalculation.bayesianEstimation(sv, training_data.sig_vec_base_v);
							PathMap.setESTxy(Coordinate.getCoord(estlocation).x, Coordinate.getCoord(estlocation).y);
							PathMap.repaint();
							printf("Estimated robot location : " + estlocation);
							printf("==============================================");
							ss.clear();
							break;
						}
					}
					// "GETAP" Auto data collection. Save the data to the database
					else if (input[0].compareToIgnoreCase("AP:") == 0) 
					{
						if (input[1].compareToIgnoreCase("END") != 0) 
						{
							ss.add(new SignalStrength(input[1], "", Integer
									.parseInt(input[2]), -1, -1));
						} 
						else 
						{
							int curr_meta_state = meta_path[meta_idx-1];
							sv = new SignalVector(ss);
							vsv.add(sv);
							printf(sv.dim + " unique AP(s) were seen!");
							vsv.add(sv);
							printf("Saving dataset for grid cell number [ " + curr_meta_state +" ]!");
							training_data.expand(vsv, curr_meta_state);
							//training_data.saveDataSet();
							ss.clear();
							vsv.clear();
							printf("Dataset successfully saved!");
							ss.clear();	
							printf("==============================================");
						}
					}
					// At the start of each movement
					else if (InMessage.length() > 5
							&& (InMessage.substring(0, 5).equalsIgnoreCase(
									"FRONT")
							|| InMessage.substring(0, 5).equalsIgnoreCase(
									"ERROR")
							|| InMessage.substring(0, 5).equalsIgnoreCase(
									"Robot")		
									)) {
						if (robot_mode == 1)			// if in auto data collection mode
						{
								if (iter < collectpath.size())
								{
									if (meta_path[meta_idx] == collectpath.get(iter))
									{
										TextInputField.setText("getap");
										sendMessage();
										TextInputField.setText("");
										iter++;
										collected = true;
									}
								}
						}
						
						// Start guiding the robot to the next cell
						int curr_meta_state = meta_path[meta_idx];
						if (meta_idx != meta_path.length -1 ) 			// if the robot is not at the destination
						{
							int next_meta_state = meta_path[++meta_idx];
							Coordinate curr_path_state = Coordinate.getCoord(curr_meta_state); 	// Display the expected position on the map
							PathMap.setRobxy(curr_path_state.x,curr_path_state.y);
							PathMap.repaint();

							curr_angle = stateTransCommand(curr_meta_state,next_meta_state, curr_angle, out); // Execute the command by first finding if turning necessary
							
							switch (curr_angle) 					// Report the orientation to the console
							{
							case 0:
								printf("Robot current orientation: "
										+ curr_angle + " (Facing North)");
								break;
							case 90:
								printf("Robot current orientation: "
										+ curr_angle + " (Facing East)");
								break;
							case 180:
								printf("Robot current orientation: "
										+ curr_angle + " (Facing South)");
								break;
							case -90:
								printf("Robot current orientation: "
										+ curr_angle + " (Facing West)");
								break;
							}
							printf("Robot current position: " + curr_meta_state);	// Report the expected position to the console
							if (robot_mode != 1 )
								printf("==============================================");
							else if (!collected)
							{
								printf("==============================================");
							}
							else
								collected = false;
						} 
						else								// if the robot reach the destination
						{
							meta_idx++;
							Coordinate Destination = Coordinate.getCoord(curr_meta_state);
							printf("Robot current position: " + curr_meta_state);	// Report the expected position to the console
							PathMap.setRobxy(Destination.x, Destination.y);			// Display the expected position on the map
							PathMap.repaint();
							printf("Robot reach the destination successfully!");
							//TODO
							if (robot_mode != 1 )									// if in self-guiding mode, start the localization function
							{
								printf("==============================================");
								TextInputField.setText("est");
								sendMessage();
								TextInputField.setText("");
							}
							else													// if in auto data collection mode, get the data for last cell and do the localization function
							{
								TextInputField.setText("getap");
								sendMessage();
								TextInputField.setText("est");
								sendMessage();
								TextInputField.setText("");
							}
						}
					}

					// The message displayed on the console
					OutMessage = comProtocol.processInput(InMessage);
					if (InMessage.compareToIgnoreCase("OK") == 0) 
					{
						printf("From client "
								+ clientSock.getInetAddress().getHostName()
								+ " : " + InMessage + " : " + OutMessage);
					} 
					else if (InMessage.compareToIgnoreCase("FINISHED") == 0) 
					{
						printf("From client "
								+ clientSock.getInetAddress().getHostName()
								+ " : " + InMessage + " : " + OutMessage);
					} 
					else if (OutMessage.compareToIgnoreCase("ERROR") == 0
							|| OutMessage.compareToIgnoreCase("Front") == 0) 
					{
						printf("From client "
								+ clientSock.getInetAddress().getHostName()
								+ " : " + InMessage);
					}
					Thread.sleep(100);
				}
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				} catch (Exception e1) 
				{
					e1.printStackTrace();
				}
				TextDisplayField.requestFocusInWindow();
			}
			
			connected = false;
			disableALL();
			if (robot_mode == 2)
			{
				printf("Self-Guiding Function Ended! Disconnecting from Client!");
			}
			else
			{
				printf("Auto Data Collection Ended! Disconnecting from Client!");
			}
			ServerClose();
			printf("Connection to the Client has been disconnected!");
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
	}

	/**
	 * Close the server connection 
	 */
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
	/**
	 * Execute the command.
	 * 
	 * @param meta1 the cell number of current position
	 * @param meta2 the cell number of next position
	 * @param angle the robot current facing angle
	 * @param out the output stream
	 * @return the int the robot new angle
	 */
	public static int stateTransCommand(Integer meta1, Integer meta2,Integer angle, PrintWriter out) 
	{
		String command1 = null; 					// Command to turn the robot
		String command2 = "MOVE 430 70"; 			// Command to move forward. [MOVE 450 10] about 25~30 cm.

		Integer[] children = Coordinate.meta_grid_graph.get(meta1); 			// Get the neighbor cell number of the current cell

		if (children[0] == meta2) 												// If next grid is at [EAST] of the current grid
		{
			if (angle != 90) 													// Check to see if the robot facing east
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 90);
				angle = 90;
			}
		} else if (children[1] == meta2) 										// If next grid is at [SOUTH] of the current grid
		{
			if (angle != 180) 													// Check to see if the robot facing south
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 180);
				angle = 180;
			}
		} else if (children[2] == meta2) 										// If next grid is at [WEST] of the current grid
		{
			if (angle != -90)													// Check to see if the robot facing west
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, -90);
				angle = -90;
			}
		}
		else if (children[3] == meta2) 											// If next grid is at [NORTH] of the current grid
		{
			if (angle != 0) 													// Check to see if the robot facing north
			{
				command1 = "TURNANGLE " + Coordinate.transAngle(angle, 0);
				angle = 0;
			}
		}

		String[] ret = { null, null };

		if (command1 != null) {
			ret[0] = command1; 					// turn command to turn the robot
			ret[1] = command2; 					// move command to move the robot
		} else {
			ret[0] = command2; 					// move command only
		}

		// Send these commands out to the robot
		for (int i = 0; i < ret.length; ++i) 
		{
			if (ret[i] != null) {
				out.println(ret[i]);
			}
		}

		// return the new angle for the robot
		return angle;
	}
}
