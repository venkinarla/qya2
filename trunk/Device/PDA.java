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

import Robot.*;
import Data.*;

public class PDA extends Thread
{
	//********** data member ************
	
	// device side communication
	private boolean dev_connected = false;
	private ServerSocket dev_server_socket;
	private Socket dev_client_socket;
	private BufferedReader din;
	private PrintWriter dout;
	
	
	// robot connection
	public Tribot lego;
	private BufferedReader rin;
	private PrintWriter rout;
	private Protocol comm_protocol;
	
	// server side communication
	public String IP = null;
	public Integer port = null;
	private boolean server_connected = false;
	private Socket server_socket;
	private BufferedReader sin;
	private PrintWriter sout;
	
	//********** class method ***********
	// initialize
	public PDA(Tribot robot)
	{
		lego = robot;
		comm_protocol = new Protocol();
		// Coordinate.initMetaGrid();
	}
	
	public PDA(Tribot robot, String IP, int port )
	{
		lego = robot;
		comm_protocol = new Protocol();
		// Coordinate.initMetaGrid();
		this.IP = IP;
		this.port = port;
	}
	
	public void setAlternative(BufferedReader in, PrintWriter out)
	{
		sin = in;
		sout = out;
	}
	
	public void run()
	{
		while (true)
		{
			if (lego != null)
			{
				if ( !lego.isConnected() )
					connectRobot();
				//if ( !dev_connected )
				//	connectDevice();
			}
			runServer();

			try
			{
				Thread.sleep(3000);
			}
			catch (InterruptedException e)
			{
				//System.err.println("error: server failed to initialize");
			}
		}
	}
	
	// create the legacy PDA-side server
	public void runServer()
	{
		// initialization
		System.out.println("Starting client...");
		// connect the PC Server 
		connectServer();
		System.out.println("Client started");
		
		while ( server_connected )
		{			
			try
			{	
				// receive pc server command
				String in_msg = sin.readLine();
				System.out.println("From PC Server: " + in_msg);
				
				if ( in_msg == null || in_msg.equalsIgnoreCase("Bye") )
				{
					System.out.println("closing server...");
					break;
				}
				
				// collect ap data and send back to server
				else if ( in_msg.length() > 5 && 
						in_msg.substring(0, 5).equalsIgnoreCase("getap") )
				{
					// e.g. getap num_sample scan_interval
					System.out.println("start getting ap data");
					String[] params = in_msg.substring(5).trim().split(" ");
					if ( params.length == 2 )
					{
						DataCollector.Scan(Integer.parseInt(params[0].trim()),
								Integer.parseInt(params[1].trim()), sout);
					}
					System.out.println("ap data collected from spotter");
					
					// use the remote device as a spotter
					/*if ( !dev_connected )
					{
						connectDevice();
					}
					dout.println(in_msg);
					System.out.println("start getting ap data");
					String dev_in_msg = din.readLine();
					while ( !dev_in_msg.equalsIgnoreCase("AP: END") )
					{
						System.out.println(dev_in_msg);
						sout.println(dev_in_msg);
						dev_in_msg = din.readLine();
					}
					System.out.println("ap data collected from spotter");*/
				}
				
				else if ( in_msg.length() > 3 &&
						in_msg.substring(0, 3).equalsIgnoreCase("run"))
				{
					lego.runProgram("GrabBall.nxj");
				}
				
				else if ( in_msg.equalsIgnoreCase("getdata") )
				{
					if ( lego.isConnected() )
					{
						//TODO
						sout.println(
							"Front : [" + lego.getFrontDist() + "]  " +
							"Left : [" + lego.getLeftDist() + "]  " + 
							"Right : [" + lego.getRightDist() + "]  " +  
							"Left speed : [" + lego.getLeftSpeed() + "]  " + 
							"Right speed : [" + lego.getRightSpeed() + "]  " +
							"Light reading : [" + lego.getFrontLight() + "]"
						);
					}
					else
					{
						sout.println("ERROR : Robot is not connected!");
					}
					
					continue;
				}

				else
				{
					// running the moving commands

					// process the command just received
					String out_msg = comm_protocol.processInput(in_msg);
					//TODO
					// execute the command
					runCommand();

					// give feedback to the server
					System.out.println("To PC Server: " + out_msg);
					sout.println(out_msg);
				}

				Thread.sleep(1000);
			}
			catch (Exception e)
			{
				System.err.println("server error: " + e.toString());
				break;
			}
		}

		// close the connection of both the robot and the PC server
		disconnectServer();
		System.out.println("server closed");
	}

	// execute the command in automated mode
	// the legacy communication interface
	public boolean runCommand()
	{
		if (comm_protocol.GetCommandListSize() > 0)
		{
			// get current command
			comm_protocol.NextCommand();
			System.out.println("Command:" + comm_protocol.GetAction());
			System.out.println("Value1:" + comm_protocol.GetValue1());
			System.out.println("Value2:" + comm_protocol.GetValue2());
			System.out.println("Duration:" + comm_protocol.GetDuration());
			
			if ( !lego.isConnected() )
			{
				System.err.println("ERROR : Robot is not connected!");
				return false;
			}

			// we could change all these to NXT side program
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
					lego.turn(
							comm_protocol.GetValue1(), 
							comm_protocol.GetValue2(), 
							comm_protocol.GetDuration());
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
					lego.setGuidingAngle(comm_protocol.GetValue1());
					//lego.turnangle(comm_protocol.GetValue1());
					lego.loggedTurnangle(comm_protocol.GetValue1());
					break;
				case Protocol.ISOBSTACLE:
					lego.runProgram("GrabBall.nxj");
					// do nothing now
					//System.out.println("go and get the ball!");
					break;
				default:
					return false;
				}
		}
		return true;
	}
	
	// connect to the robot
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
				System.out.println("Initializing PC Server connection...");

				server_socket = new Socket(IP, port);
				sin = new BufferedReader(new InputStreamReader(server_socket
						.getInputStream()));
				sout = new PrintWriter(server_socket.getOutputStream(), true);
				server_connected = true;
				System.out.println("PC Server connection initialized");
				break;
			}
			catch (Exception e1)
			{
				System.out.println("Connection error: " + e1.toString());
			}

			try
			{
				Thread.sleep(3000);
			}
			catch (Exception e)
			{
				// do nothing
			}
		}
		
		return server_connected;
	}
	
	
	protected void finalize()
	{
		//disconnectDevice();
		disconnectRobot();
		disconnectServer();
	}
	
	public boolean connectRobot()
	{
		try
		{
			lego.configure();
			rin = new BufferedReader( new InputStreamReader(lego.getInputStream()) );
			rout = new PrintWriter(lego.getOutputStream());
		}
		catch (Exception e)
		{
			System.err.println("connection error: cannot connect to the robot\n"+ e.toString());
		}
		return lego.isConnected();
	}
	
	
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
	
	// close the connection
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
	
	// connect to a remote wifi spotter 
	// listen to port 520
	/*public void connectDevice()
	{
		while ( dev_connected )
		{
			disconnectDevice();
		}
		
		System.out.println("Waiting for Spotter Device connection...");
		while ( !dev_connected )
		{
			try
			{
				dev_server_socket = new ServerSocket(520);
				dev_client_socket = dev_server_socket.accept();
				din = new BufferedReader(new InputStreamReader(
						dev_client_socket.getInputStream()));
				dout = new PrintWriter(dev_client_socket.getOutputStream(),true);
				System.out.println("Spotter Device connection established");
				dev_connected = true;
			}
			catch (IOException e)
			{
				System.err.println("error: cannot bind a client socket\n"+ e.toString());
			}
			
			try
			{
				Thread.sleep(3000);
			}
			catch (InterruptedException e)
			{
				//
			}
		}
	}
	
	public void disconnectDevice()
	{
		while ( dev_connected )
		{
			try
			{
				din.close();
				dout.close();
				dev_client_socket.close();
				dev_server_socket.close();
				dev_connected = false;
				System.out.println("Spotter Device connection established");
			}
			catch (IOException e)
			{
				System.err.println("error: cannot close spotter connection\n" + e.toString());
			}
		}
	}*/
	
	
	public static void main(String[] args)
	{
		PDA pda = new PDA(new Tribot());
		pda.start();
	}
}
