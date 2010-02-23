package Data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;

import LocationEstimation.LocEstim;
import State.Coordinate;


public class SignalDatabase 
{
	//********************* data member ************************
	public static String dataset_file = "src/dataset/dataset.txt";
	public static String dataset_file2 = "src/dataset/dataset2.txt";
	public Vector<SignalVector> sig_vec_base;
	public Vector<Vector<SignalVector>> sig_vec_base_v;
	
	// answer for location estimation queries
	public LocEstim loc_estim; 
	
	//********************* class method ***********************
	// initialization
	public SignalDatabase()
	{
		sig_vec_base = new Vector<SignalVector>();
		sig_vec_base_v = new Vector<Vector<SignalVector>>();
		sig_vec_base_v.setSize(106);
		for( int i=0; i<106; i++ )
			sig_vec_base_v.setElementAt(new Vector<SignalVector>(), i);
		
		/*SignalVector temp = new SignalVector();
		sig_vec_base_v.elementAt(50).add(temp);
		if( sig_vec_base_v.get(50) == null )
			System.out.println("Is null");
		
		System.out.println(sig_vec_base_v.size()+" "+sig_vec_base_v.capacity());*/
	}
	
	public void initDatabase()
	{
		loadDataSet();
	}
	
	// using kNN right now
	public Integer getLocation( SignalVector query )
	{
		return loc_estim.knn_estimate(query, sig_vec_base, 7);
	}
	
	// take an average of all the vectors
	public static Vector<SignalStrength> dataFusion( Vector<Vector<SignalStrength>> dataset )
	{
		Vector<SignalStrength> fused_data = new Vector<SignalStrength>();
		
		for ( int i=0; i<dataset.size(); ++i )
		{
			Vector<SignalStrength> curr_vec = dataset.elementAt(i);
			for ( int j=0; j<curr_vec.size(); ++j )
			{
				fused_data.add(curr_vec.elementAt(j));
			}
		}
		
		return preProcess( fused_data );
	}
	
	// add the new training data to database
	public void expand(Vector<SignalVector> novo_dataset, int meta_grid)
	{
		for ( int i=0; i<novo_dataset.size(); ++i )
		{
			SignalVector sig_vec = novo_dataset.get(i);
			sig_vec.meta_grid = meta_grid;
			sig_vec_base.add(sig_vec);
		}
	}
	
/*	public void preProcess()
	{
		for ( int i=0; i<signal_data.size(); ++i )
		{
			Vector<SignalStrength> raw_vec = signal_data.elementAt(i);
			signal_data.set(i, SignalDatabase.preProcess(raw_vec));
		}
	}*/
	
	
	// preproces the data, take the average of all
	// the result vector will contain no duplicate elements
	public static Vector<SignalStrength> preProcess( Vector<SignalStrength> raw_vec )
	{
		Vector<SignalStrength> curr_vec = raw_vec;
		Map<String, Integer> avg_map = new HashMap<String, Integer>();
		Map<String, Integer> avg_count = new HashMap<String, Integer>();
		Map<String, SignalStrength> avg_rec = new HashMap<String, SignalStrength>();
		for (int j = 0; j < curr_vec.size(); ++j)
		{
			SignalStrength curr_sig = curr_vec.elementAt(j);
			String curr_mac = curr_sig.Mac_address;
			int curr_rssi = curr_sig.Signal_Strength;
			if (avg_map.containsKey(curr_mac))
			{
				avg_map.put(curr_mac, avg_map.get(curr_mac) + curr_rssi);
				avg_count.put(curr_mac, avg_count.get(curr_mac) + 1);
			}
			else
			{
				avg_map.put(curr_mac, curr_rssi);
				avg_count.put(curr_mac, 1);
				avg_rec.put(curr_mac, curr_sig);
			}
		}
		curr_vec.clear();
		for (String str : avg_map.keySet())
		{
			int avg_val = (int) (avg_map.get(str) / (double) avg_count.get(str));
			SignalStrength this_sig = avg_rec.get(str);
			this_sig.Signal_Strength = avg_val;
			curr_vec.add(this_sig);
		}
		return curr_vec;
	}
	
	// post process the data
	// group the small grids into big one
	
	// load the training data
	public void loadDataSet()
	{
		Scanner fin = null;
		try
		{
			fin = new Scanner(new FileInputStream(dataset_file));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("loading signal data error: the file " + 
					dataset_file + " is not found." );
			System.err.println(e.toString());
			return;
		}
		
		boolean start = false;
		Integer meta_grid = 0;
		Integer dim = 0;
		while ( fin.hasNextLine() )
		{
			String curr_read = fin.nextLine().trim();
			
			if ( curr_read.length() == 0 )
			{
				start = true;
				//start = false;  to read new dataset format
			}
			else if ( curr_read.charAt(0) == '%' )
			{
				// do nothing
			}
			else if ( curr_read.charAt(0) == '#' )
			{
				start = false;
				//start = true; to read new dataset format
			}
			else if ( start )
			{
				if( curr_read.charAt(0)=='#' ){
					start = false;
					continue;
				}
				start = false;
				dim = Integer.parseInt(curr_read.split(" ")[0]);
				meta_grid = Integer.parseInt(curr_read.split(" ")[1]);
				sig_vec_base_v.elementAt(meta_grid-1).add(new SignalVector());
				sig_vec_base_v.elementAt(meta_grid-1).lastElement().dim = dim;
				sig_vec_base_v.elementAt(meta_grid-1).lastElement().meta_grid = meta_grid;
				
				sig_vec_base.add(new SignalVector());
				sig_vec_base.lastElement().dim = 
					Integer.parseInt(curr_read.split(" ")[0]);
				sig_vec_base.lastElement().meta_grid = 
					Integer.parseInt(curr_read.split(" ")[1]);
			}
			else
			{	
				String[] reading = curr_read.split(" ");
				sig_vec_base_v.elementAt(meta_grid-1).lastElement().put(
						reading[0], Integer.parseInt(reading[1]));
				sig_vec_base.lastElement().put(
						reading[0], Integer.parseInt(reading[1]));
			}
		}
	}
	
	// save the training data
	public void saveDataSet()
	{
		PrintWriter fout = null;
		PrintWriter fout2 = null;
		try
		{
			fout = new PrintWriter( dataset_file );
			fout2 = new PrintWriter( dataset_file2 );
		}
		catch (FileNotFoundException e)
		{
			System.err.println("saving signal data error: the file"
					+ dataset_file + " cannot be found");
			System.err.println(e.toString());
			return;
		}
		
		fout.println("% do not modify the content");
		fout.println("% generated by machine");
		fout.println("% format: dimension   meta_grid");
		fout.println("%         bssid       signal_strength");
		fout.println("%         ......      ......");
		fout.println();
		
		fout2.println("% do not modify the content");
		fout2.println("% generated by machine");
		fout2.println("% format: dimension   meta_grid");
		fout2.println("%         bssid       signal_strength");
		fout2.println("%         ......      ......");
		fout2.println();
		
		
		System.out.println("Now saving data");
		for ( int i=0; i<sig_vec_base.size(); ++i )
		{
			SignalVector curr_vec = sig_vec_base.get(i);
			
			// we only take meaningful result into account
			if ( curr_vec.dim < 6 )
				continue;
			
			// format:$dimension $meta_grid
			fout.println(curr_vec.dim + " " + curr_vec.meta_grid);
			for ( String mac_addr : curr_vec.getMacAddr() )
			{
				// format: $mac_addr $rssi
				fout.println(mac_addr + " " + curr_vec.getRSSI(mac_addr));
			}
			fout.println();
		}
		
		for( int i=0; i<sig_vec_base_v.size(); i++ ){
			Vector<SignalVector> curr_vec_vec = sig_vec_base_v.elementAt(i);
			//When no records for a specific grid
			if( curr_vec_vec.size()==0 ){
				fout2.println("#"+i+1+" NO DATA ");
			}
			else{
				for( int j=0; j<curr_vec_vec.size(); ++j ){
					fout2.println("#Grid "+ (i+1) + " Record "+(j+1));
					SignalVector curr_vec = curr_vec_vec.get(j);			
					// we only take meaningful result into account
					if ( curr_vec.dim < 6 )
						continue;
					// format:$dimension $meta_grid
					fout2.println(curr_vec.dim + " " + curr_vec.meta_grid);
					for ( String mac_addr : curr_vec.getMacAddr() )
					{
						// format: $mac_addr $rssi
						fout2.println(mac_addr + " " + curr_vec.getRSSI(mac_addr));
					}
					fout2.println();
				}
			}
			for(int k=curr_vec_vec.size()+1; k<=4; k++ ){
				fout2.println("#Grid "+ (i+1) + " Record "+k+" is missing.");
				fout2.println();
			}
		}
		fout2.close();
		fout.close();
		System.out.println("data saved");
	}
	
	public static void main(String[] args)
	{
		SignalDatabase database = new SignalDatabase();
		database.loadDataSet();
		
		database.saveDataSet();
	}
	
}
