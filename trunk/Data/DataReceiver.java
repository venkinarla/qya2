/**
 * Receive the WiFi data message from a remote data collector
 */

package Data;

import java.net.*;
import java.io.*;
import java.util.*;

public class DataReceiver extends Thread
{
	//**************** data member *****************
	ServerSocket server_socket;
	PrintWriter dout;
	BufferedReader din;
	int port;
	
	//**************** member method ***************
	// initialization
	public DataReceiver(int port)
	{
		this.port = port;
	}
	
	public void run()
	{
		runServer();
	}
	
	public void runServer()
	{
		Socket socket = new Socket();
		try 
		{	
			server_socket = new ServerSocket(port);
			socket = server_socket.accept();
			din = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			dout = new PrintWriter(socket.getOutputStream(), true);
		} 
		catch (IOException e) 
		{
			System.out.println("error: cannot establish connection");
		}

		System.out.println("server: connection established");
		System.out.println("remote device address:\n" + 
				socket.getRemoteSocketAddress().toString());
		
		int num_sample = 5;
		int scan_interval = 3000;
		
		// listen to the message exchange request
		// according to the data exchange protocol
		while ( true )
		{
			dout.println("getap " + num_sample + " " + scan_interval);
			try
			{
				String in_msg = din.readLine();
				while ( !in_msg.equalsIgnoreCase("AP: END") )
				{
					System.out.println(in_msg);
					in_msg = din.readLine();
				}
			}
			catch ( IOException e )
			{
				System.err.println("error: cannot get incoming message\n" + e.toString());
				break;
			}
		}
		try 
		{
			System.out.println("disconnecting device...");
			socket.close();
		} 
		catch (IOException e) 
		{
			System.err.println("error: cannot close the socket\n" + e.toString());
		}
		System.out.println("device disconnected");
	}
	
	
	public static void main(String[] args)
	{
		DataReceiver receiver = new DataReceiver(520);
		DataCollector collector = new DataCollector("localhost", 520);
		receiver.start();
		collector.start();
	}
}
