/**
 * obtain the WiFi signal data by scanning the area
 * this is equipped on a device with 802.11a/b/g chip
 */

package Data;

import java.util.*;
import java.io.*;
import java.net.*;

import org.placelab.core.*;
import org.placelab.eventsystem.EventListener;
import org.placelab.eventsystem.EventSystem;
import org.placelab.eventsystem.SimpleEventSystem;
import org.placelab.spotter.*;

public class DataCollector extends Thread
{
	//************ data member ****************
	private static final int MAX_NUM_SAMPLE = 20;
	private static final int MIN_NUM_SAMPLE = 1;
	private static final int MAX_INTERVAL = 10000;
	private static final int MIN_INTERVAL = 2000;
	
	private Socket socket;
	private BufferedReader sin;
	private PrintWriter sout;
	private boolean connected = false;
	
	String IP = "localhost";
	int port = 520;
	
	//************ class method ****************
	// initialization
	public DataCollector(String IP, int port)
	{
		this.IP = IP;
		this.port = port;
	}
	
	public void run()
	{
		while ( true )
		{
			connect();
			runServer();
			disconnect();
		}
	}

	private void runServer()
	{
		while (true)
		{
			try
			{			
				String in_msg = sin.readLine();	
				if ( in_msg == null )
					break;
					
				if ( in_msg.length() > 5 &&
						in_msg.substring(0, 5).equalsIgnoreCase("getap"))
				{
					// format: GETAP NUM_SAMPLES SCANNING_INTERVAL
					String[] params = in_msg.substring(5).trim().split(" ");
					if ( params.length == 2 )
					{
						DataCollector.Scan(Integer.parseInt(params[0].trim()),
								Integer.parseInt(params[1].trim()), sout);
					}
					else
					{
						System.err.println("command format error: we need two parameters");
						continue;
					}
				}
			}
			catch (IOException e)
			{
				System.err.println("spotter error: I/O disruption\n" + e.toString());
				break;
			}

			try
			{
				Thread.sleep(3000);
			}
			catch (InterruptedException e)
			{
				System.err.println("spotter error: interrupted\n" + e.toString());
				break;
			}
		}
		System.out.println("server stopped");
	}
	
	// the warper for the other scan function
	public static void Scan( int num_sample, int interval, PrintWriter out )
	{
		//out.println("AP: START");
		Vector<SignalVector> vecs = Scan(num_sample, interval);
		if (vecs == null)
		{
			do
			{
				/*try {
					Runtime.getRuntime().exec("devcon disable \"PCI\\VEN_8086&DEV_4220*\"");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Runtime.getRuntime().exec("devcon enable \"PCI\\VEN_8086&DEV_4220*\"");
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					out.println("ERROR : Please restart!");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					vecs = Scan(num_sample, interval);				
				/*} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			} while (vecs == null);
		}
		
		for ( int i=0; i<vecs.size(); ++i )
		{
			SignalVector sig_vec = vecs.get(i);
			//System.out.println(sig_vec.dim + " AP(s) found:");
			for ( String mac_addr : sig_vec.getMacAddr() )
			{
				out.println("AP: " + mac_addr + " " + sig_vec.getRSSI(mac_addr));
			}
		}
		out.println("AP: END");
	}
	
	public static void estimate( int num_sample, int interval, PrintWriter out )
	{
		Vector<SignalVector> vecs = null;
		SignalVector sv = new SignalVector();
		do
		{
			vecs =Scan(num_sample, interval);
		}while (vecs == null);
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		HashMap<String,Integer> counter = new HashMap<String,Integer>();
		for( int i=0; i < 10; i++)
		{
			sv = vecs.elementAt(i);
			for( String key : sv.vec.keySet()){
				if( map.containsKey(key)){
					map.put(key, map.get(key)+sv.getRSSI(key));
					counter.put(key, counter.get(key)+1);
				}
				else{
					map.put(key, sv.getRSSI(key));
					counter.put(key,1);
				}
			}
		}
		for( String key: map.keySet()){
			map.put(key, map.get(key)/counter.get(key));
		}
		
		for ( int i=0; i < vecs.size(); ++i )
		{
			//SignalVector sig_vec = vecs.get(i);
			for ( String mac_addr : map.keySet() )
			{
				out.println("EST: " + mac_addr + " " + map.get(mac_addr));
			}
		}
		out.println("EST: END");
	}
	
	/**
	 * the warper for the other scan function
	 * @param num_sample
	 * @param interval
	 * @param out
	 */
	public static void ScanVirtual( int num_sample, int interval, PrintWriter out )
	{
		//SignalDatabase db = new SignalDatabase();
		String signal = "src/dataset/VirtualSignal";
		Vector<SignalStrength> ss = new Vector<SignalStrength>();
		SignalVector sv =  new SignalVector();
		
		//db.loadDataSet();
		
		Scanner fin = null;
		try
		{
			fin = new Scanner(new FileInputStream(signal));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("loading signal data error: the file " + 
					signal + " is not found." );
			System.err.println(e.toString());
			return;
		}
		
		Vector<String> lines = new Vector<String>();
		String mac, name;
		int rssi, x, y;
		while( fin.hasNextLine()){
			lines.add(fin.nextLine().trim());
		}
		
		for( int i=0; i<lines.size(); i++ ){
			String[] reading = lines.elementAt(i).split(" ");
			mac = reading[0];
			rssi = Integer.parseInt(reading[1]);
			name = "Test";
			x = 1;
			y = 1;
			ss.add(new SignalStrength(mac, name, rssi, x, y));
		}
		sv = new SignalVector(ss);
		
		for ( String mac_add : sv.getMacAddr() )
		{
			out.println("AP: " + mac_add + " " + sv.getRSSI(mac_add));
			
		}
		out.println("AP: END");
	}
	
	public static void ScanVirtual2( int num_sample, int interval, PrintWriter out )
	{
		//SignalDatabase db = new SignalDatabase();
		String signal = "src/dataset/VirtualSignal";
		Vector<SignalStrength> ss = new Vector<SignalStrength>();
		SignalVector sv =  new SignalVector();
		
		//db.loadDataSet();
		
		Scanner fin = null;
		try
		{
			fin = new Scanner(new FileInputStream(signal));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("loading signal data error: the file " + 
					signal + " is not found." );
			System.err.println(e.toString());
			return;
		}
		
		Vector<String> lines = new Vector<String>();
		String mac, name;
		int rssi, x, y;
		while( fin.hasNextLine()){
			lines.add(fin.nextLine().trim());
		}
		
		for( int i=0; i<lines.size(); i++ ){
			String[] reading = lines.elementAt(i).split(" ");
			mac = reading[0];
			rssi = Integer.parseInt(reading[1]);
			name = "Test";
			x = 1;
			y = 1;
			ss.add(new SignalStrength(mac, name, rssi, x, y));
		}
		sv = new SignalVector(ss);
		
		for ( String mac_add : sv.getMacAddr() )
		{
			out.println("EST: " + mac_add + " " + sv.getRSSI(mac_add));
			
		}
		out.println("EST: END");
	}
	// scan
	// return format: 
	//			AP: MAC_ADDR SSID RSSI
	//				..................
	//			AP: END
	// Due to uncertainty of mobile device, we will be using the raw data directly
	// since we don't quite care about the "righteous" value. What we want is stability
	public static Vector<SignalVector> Scan( int num_sample, int interval )
	{	
		if (num_sample <= MIN_NUM_SAMPLE && num_sample > MAX_NUM_SAMPLE)
		{
			System.err.println("error: invalid samples");
			return null;
		}
		if (interval <= MIN_INTERVAL && interval > MAX_INTERVAL)
		{
			System.err.println("error: invalid scanning interval ");
			return null;
		}

		Vector<SignalVector> novo_vecs = new Vector<SignalVector>();
		
		WiFiSpotter collector = new WiFiSpotter();
		try
		{
			//System.out.println("Starting Getting AP Data!");
			for (int j = 0; j < num_sample; j++)
			{	
				collector.open();
				String[] raw_data = collector.spotter_poll();
				SignalVector sig_vec = new SignalVector(preProcess(raw_data));
				
				//System.out.println(sig_vec.dim + " APs were seen");
				if (sig_vec.dim > 0)
				{
					// Iterate through the Vector and print the readings
					//for ( String mac_addr : sig_vec.getMacAddr() )
					//{
						/*out.println("AP: " + " " +
								mac_addr + " " +
								sig_vec.getRSSI(mac_addr));*/
					
						novo_vecs.add(sig_vec);
					//}
				}
				collector.close();
				//Thread.sleep(interval);
			}
			//out.println("AP: END");
			//System.out.println(novo_vecs.size());
			
		}
		catch (Exception ex)
		{
			System.err.println("System Error: " + ex.getMessage());
			return null;
		}
		
		return novo_vecs;
	}
	
	// the raw data for each signal strength datum consists of 5 lines
	// bssid, ssid, signal_strength, wep_enable, ...
	protected static Vector<SignalStrength> preProcess( String[] raw_data )
	{
		Vector<SignalStrength> novo_vec = new Vector<SignalStrength>();
		for ( int i=0; i<=raw_data.length-5; i+=5 )
		{
			String bssid = raw_data[i];
			String ssid = raw_data[i+1];
			Integer sig_val = Integer.parseInt(raw_data[i+2]);
			novo_vec.add(new SignalStrength(bssid, ssid, sig_val, -1, -1));
			//System.out.println(novo_vec);
		}
		return novo_vec;
	}
	
	// ************************ connection method ***********************
	public void connect()
	{
		if ( connected )
		{
			disconnect();
		}
		
		System.out.println("spotter connecting to server...");
		
		while (!connected)
		{
			try
			{
				socket = new Socket(IP, port);
				sin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				sout = new PrintWriter(socket.getOutputStream(), true);
				connected = true;
				System.out.println("device: connection established");
				System.out.println("server address:\n" + 
						socket.getRemoteSocketAddress());
			}
			catch (Exception e)
			{
				System.out.println("connection error: cannot establish\n " + e.toString());
			}
		}
	}
	
	public void disconnect()
	{
		while ( connected )
		{
			try
			{
				sin.close();
				sout.close();
				socket.close();
				connected = false;
			}
			catch (IOException e)
			{
				System.err.println("error: cannot disconnect\n" + e.toString());
			}
		}
	}
	
	public static void main(String[] args)
	{
		DataCollector collector = new DataCollector("localhost", 520);
		collector.start();
	}
	
	
}
