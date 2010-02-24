/**
 * A representation of Signal Strength Vector 
 */

package Data;

import java.util.*;

import State.Coordinate;

public class SignalVector
{
	//************ data member *************
	public Integer dim;
	public Integer meta_grid; // where we got this signal vector
	public Map<String, Integer> vec;
 	
	//************ class method ************
	// initialization
	public SignalVector()
	{
		meta_grid = -1;
		dim = -1;
		vec = new HashMap<String, Integer>();
	}
	
	// conversion constructor
	public SignalVector(Vector<SignalStrength> raw_vec)
	{
		vec = new HashMap<String, Integer>();
		raw_vec = SignalDatabase.preProcess(raw_vec);
		for ( int i=0; i<raw_vec.size(); ++i )
		{
			// mac address could uniquely distinguish access points, thus only this is needed
			vec.put(raw_vec.get(i).Mac_address, 
					raw_vec.get(i).Signal_Strength);
		}
		dim = raw_vec.size();
		meta_grid = Coordinate.getGridNum(
				new Coordinate(raw_vec.firstElement().x, 
							   raw_vec.firstElement().y));
	}
	
	public void put(String mac_addr, Integer rssi)
	{
		if ( vec != null )
			vec.put(mac_addr, rssi);
	}
	
	// return the set of mac address in the current signal vector
	public Set<String> getMacAddr()
	{
		if ( vec != null )
			return vec.keySet();
		else
			return null;
	}
	
	// get signal with corresponding mac address
	public int getRSSI(String mac_addr)
	{
		if ( vec.containsKey(mac_addr) )
			return vec.get(mac_addr);
		else
			return 0;
	}
	
	// convert the old data set to this new one
	public static Vector<SignalVector> getSigVec(Vector<Vector<SignalStrength>> old_dataset)
	{
		Vector<SignalVector> novo_dataset = new Vector<SignalVector>();
		
		for ( int i=0; i<old_dataset.size(); ++i )
		{
			novo_dataset.add(new SignalVector(old_dataset.get(i)));
		}
		
		return novo_dataset;
	}
	
	// Euclidean Distance for the two signal vectors 
	public static double distEuclidean( SignalVector vec1, SignalVector vec2 )
	{
		double dist = 0.0;
		Set<String> key_set =vec1.vec.keySet();
		//key_set.retainAll(vec2.vec.keySet());
		
		for ( String mac_addr : key_set )
		{
			//System.out.println(mac_addr);
			dist += Math.sqrt( Math.abs( vec2.getRSSI(mac_addr) - vec1.getRSSI(mac_addr) ));
		}
		return dist;
	}
	
	public static double distManhattan( SignalVector vec1, SignalVector vec2 )
	{
		double dist = 0.0;
		Set<String> key_set =vec1.vec.keySet();
		key_set.retainAll(vec2.vec.keySet());
		for ( String mac_addr : key_set )
		{
			dist += Math.abs( vec2.getRSSI(mac_addr) - vec1.getRSSI(mac_addr) );
		}
		return dist;
	}

}

