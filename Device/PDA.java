package Device;

/**
 * Deal with these things
 * 		1. Answer to robot's request for location estimation
 * 		2. Request that the robot stop and run automated 
 * 		3. Request the robot to stop and turn to manual control
 * 
 * @author Philip
 *
 */

import java.io.*;
import java.net.*;

import javax.swing.JFrame;

import Robot.*;
import Data.*;
import GUI.UI_PDA;

/**
 * The Class PDA.
 */
public class PDA extends Thread
{
	public Tribot lego;						// The lego robot
	private BufferedReader rin;				// The Input stream of the robot
	private PrintWriter rout;				// The Output stream of the robot
	private Protocol comm_protocol;			// The communication protocol
	

	public String IP = null;					// The IP address
	public Integer port = null;					// The Port number
	private boolean server_connected = false;	// The boolean value to tell if the server is connected
	private Socket server_socket;				// The server side socket
	private BufferedReader sin;					// The Input stream of the server
	private PrintWriter sout;					// The Output stream of the server
	
	/**
	 * Instantiates a PDA object.
	 * 
	 * @param robot the robot
	 */
	public PDA(Tribot robot)
	{
		lego = robot;
		comm_protocol = new Protocol();
		// Coordinate.initMetaGrid();
	}
	
	/**
	 * Instantiates a PDA object.
	 * 
	 * @param robot the robot
	 * @param IP the IP
	 * @param port the port
	 */
	public PDA(Tribot robot, String IP, int port )
	{
		lego = robot;
		comm_protocol = new Protocol();
		// Coordinate.initMetaGrid();
		this.IP = IP;
		this.port = port;
	}
	
	/**
	 * Sets the alternative.
	 * 
	 * @param in the in
	 * @param out the out
	 */
	
	/**
	 * This is run by creating a new thread
	 * 
	 */
	public void run()
	{
		while (true)
		{
			if (lego != null)
			{
				if ( !lego.isConnected() )
				{
					connectRobot();
				}
			}
			runServer();
		}
	}
	

	/**
	 * Start the PDA client and connect to the server
	 */
	public void runServer()
	{
		// initialization
		//System.out.println("Starting client...");
		// connect the PC Server 
		connectServer();
		if (server_connected)
			System.out.println("Server Connected!");
		
		while ( server_connected )										// After connected, wait for input command
		{			
			try
			{	
				String in_msg = sin.readLine();
				String [] input = in_msg.split(" ");
					System.out.println("From PC Server: " + in_msg);
				if ( in_msg == null || in_msg.equalsIgnoreCase("Bye") )			// "BYE" or null
				{
					System.out.println("Closing connection...");
					break;
				}
				else if (in_msg.equalsIgnoreCase("EST"))						// "EST"
				{
					//sout.println("Estimating robot location, Please Wait...");
					DataCollector.estimate(10, 1000, sout);
				}
				// collect ap data and send back to server
				else if (input[0].equalsIgnoreCase("getap"))					
				{
					// e.g. getap num_sample scan_interval						// "GETAP"
					//System.out.println("==============================");
					//System.out.println("Start getting signal data!");
					//sout.println("Collecting APs signal, Please Wait...");
					DataCollector.Scan(1, 1000, sout);
					//System.out.println("Signal data collected successfully!");
					//System.out.println("==============================");
				}
				else if (in_msg.equalsIgnoreCase("getdata"))					// "GETDATA"
				{
					if ( lego.isConnected() )
					{
						//TODO
						sout.println(
							"Front : [" + lego.getFrontDist() + "]  " +
							"Left : [" + lego.getLeftDist() + "]  " + 
							"Right : [" + lego.getRightDist() + "]  " +  
							"Left speed : [" + lego.getLeftSpeed() + "]  " + 
							"Right speed : [" + lego.getRightSpeed() + "]  " 
							//"Light reading : [" + lego.getFrontLight() + "]"
						);
					}
					else
					{
						sout.println("ERROR : Robot is not connected!");
					}
					continue;
				}
				else if ( in_msg.equalsIgnoreCase("isobstacle"))				// "ISOBSTACLE"
				{
					if ( lego.isConnected() )
					{
						int front = lego.getFrontDist();
						int left = lego.getLeftDist();
						int right = lego.getRightDist();
												
						sout.println(
							"Front : [" + front + "]  " +
							"Left : [" + left + "]  " + 
							"Right : [" + right + "]"
						);
						if (front <= 40)
							sout.println("Warning : Obstacle detected at the FRONT of the robot!");
						if (left <= 40)
							sout.println("Warning : Obstacle detected at the LEFT side of the robot!");
						if (right <= 40)
							sout.println("Warning : Obstacle detected at the RIGHT side of the robot!");
					}
					else
					{
						sout.println("ERROR : Robot is not connected!");
					}
					continue;					
				}
				else if ( in_msg.equalsIgnoreCase("temp"))						// "TEMP"
				{
					sout.println("Robot travelling...");
					continue;
				}
				else
				{
					// process the command just received
					String out_msg = comm_protocol.processInput(in_msg);
					runCommand();
					sout.println(out_msg);										// Send response to the server
				}

				Thread.sleep(1000);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				break;
			}
		}
		disconnectServer();
		System.out.println("Connection to the server has been closed!");
	}

	/**
	 * Run command.
	 * 
	 * @return true, if successful
	 */
	public boolean runCommand()
	{
		if (comm_protocol.GetCommandListSize() > 0)
		{
			comm_protocol.NextCommand();										// Get the next command
			/*System.out.println("Command:" + comm_protocol.GetAction());
			System.out.println("Value1:" + comm_protocol.GetValue1());
			System.out.println("Value2:" + comm_protocol.GetValue2());
			System.out.println("Duration:" + comm_protocol.GetDuration());*/
			
			if ( !lego.isConnected() )
			{
				//System.err.println("ERROR : Robot is not connected!");
				return false;
			}
			switch (comm_protocol.GetAction())
				{
				case Protocol.MOVE:
					lego.forward(comm_protocol.GetValue1(),
							comm_protocol.GetDuration());
					break;
				case Protocol.STOP:
					lego.stop();
					break;
				case Protocol.TURN:
					/*lego.turn(
							comm_protocol.GetValue1(), 
							comm_protocol.GetValue2(), 
							comm_protocol.GetDuration());*/
				case Protocol.MOVEDIST:
					/*lego.moveDistance(
							comm_protocol.GetValue1(), 
							comm_protocol.GetValue2());*/
					break;
				case Protocol.GETAP:
					// get ap from remote device
					DataCollector.Scan(
							comm_protocol.GetValue1(), 
							comm_protocol.GetValue2(), 
							sout);
					break;
				case Protocol.TURNANGLE:
					// change the orientation of the robot as well
					//lego.setOrientation(lego.getOrientation() + comm_protocol.GetValue1());
					//lego.turnangle(comm_protocol.GetValue1());
					//lego.loggedTurnangle(comm_protocol.GetValue1());
					break;
				case Protocol.ISOBSTACLE:
					//lego.runProgram("GrabBall.nxj");
					// do nothing now
					//System.out.println("go and get the ball!");
					break;
				default:
					return false;
				}
		}
		return true;
	}
	
	/**
	 * Connect to the server.
	 * 
	 * @return true, if successful
	 */
	public boolean connectServer()
	{	
		// close the previous PC server connection
		if (server_connected)
		{
			disconnectServer();
		}
		
		System.out.println("Starting PC Server connection...");
		
		while (IP == null && port == null)
		{
			try
			{
				BufferedReader sysin = new BufferedReader(
						new InputStreamReader(System.in));

				System.out.print("IP: ");
				IP = sysin.readLine();
				System.out.print("Port: ");
				port = Integer.parseInt(sysin.readLine());
			}
			catch (IOException e)
			{
				System.err.println("ERROR: cannot read IP and Port info\n"
						+ e.toString());
			}
		}
		

		while (!server_connected)
		{
			try
			{
				//System.out.println("Initializing PC Server connection...");

				server_socket = new Socket(IP, port);
				//System.out.println(IP + " " + port);
				sin = new BufferedReader(new InputStreamReader(server_socket
						.getInputStream()));
				sout = new PrintWriter(server_socket.getOutputStream(), true);
				server_connected = true;
				//System.out.println("PC Server connection initialized");
				break;
			}
			catch (Exception e1)
			{
				System.out.println("Connection Error: " + e1.getMessage());
			}

			try
			{
				Thread.sleep(10000);
			}
			catch (Exception e)
			{
			}
		}
		
		return server_connected;
	}
	
	/**
	 * Connect to the robot.
	 */
	public void connectRobot()
	{
		boolean connected = false;
		try
		{
			connected = lego.configure();
			if (connected)
			{
				rin = new BufferedReader( new InputStreamReader(lego.getInputStream()) );
				rout = new PrintWriter(lego.getOutputStream());
			}
		}
		catch (Exception e)
		{
			System.err.println("Connection error: Cannot connect to the robot\n"+ e.toString());
		}

	}
	
	
	/**
	 * Disconnect from the robot.
	 * 
	 * @return true, if successful
	 */
	public boolean disconnectRobot()
	{
		try
		{
			rin.close();
			rout.close();
			lego.disconnect();
		}
		catch (Exception e)
		{
			System.err.println("error: cannot close robot connection\n" + e.toString());
		}
		return lego.isConnected();
	}
	
	/**
	 * Disconnect from the server.
	 * 
	 * @return true, if successful
	 */
	public boolean disconnectServer()
	{
		while (server_connected)
		{
			try
			{
				sin.close();
				sout.close();
				server_socket.close();
				server_connected = false;
			}

			catch (Exception e)
			{
				System.err.println("error: cannot close connection\n" + e.toString());
			}
		}
		return server_connected;
	}
	
	/**
	 * Sets the IP.
	 * 
	 * @param IP the new IP
	 */
	public void setIP(String IP){
		this.IP = IP;
	}
	
	/**
	 * Sets the port.
	 * 
	 * @param port the new port
	 */
	public void setPort(int port){
		this.port = port;
	}
	
	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args)
	{
		PDA pda = new PDA(new Tribot());
		pda.start();
	}
}

