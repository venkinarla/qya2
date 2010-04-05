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
		//key_set.retainAll(vec2.vec.keySet());
		
		/*
		 * Sorting the hash map in order to support 10 strongest signals
		 */
		SignalVector temp = new SignalVector();
		for( String key : vec1.vec.keySet()){
			temp.vec.put(key, vec1.getRSSI(key));
		}
		List mapKeys = new ArrayList(temp.vec.keySet());
		List mapValues = new ArrayList(temp.vec.values());
		Collections.sort(mapValues);
		Collections.sort(mapKeys);
		
		LinkedHashMap lnkMap = new LinkedHashMap();
		Iterator valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Integer val = (Integer)valueIt.next();
			Iterator keyIt = mapKeys.iterator();
			while (keyIt.hasNext()) {
				String key = (String)keyIt.next();
				if (temp.vec.get(key).toString().equals(val.toString())) {
					temp.vec.remove(key);
					mapKeys.remove(key);
					lnkMap.put(key, val);
					break;
				}
			}
		}
		
		//for( String tmp: vec2.vec.keySet()){
		//	System.out.println("vec2 "+tmp+" "+vec2.getRSSI(tmp));
		//}
		/*Object[] test = lnkMap.keySet().toArray();
		for( int i=0; i<test.length; i++ ){
			//System.out.println("object "+i+" *"+test[i].toString()+"* "+vec2.getRSSI(test[i].toString()));
			for( String mac: vec2.vec.keySet()){
				if(! mac.equals(test[i].toString()))
					System.out.println("vec2 *"+mac+"* *"+test[i].toString()+"*" );
				else System.out.println("!!  *"+mac+"* *"+test[i].toString()+"*" );
			}
		}
		*/
		Iterator keyIt = lnkMap.keySet().iterator();
		int c=1;
		while( keyIt.hasNext()){
			if( c>10) break;
			String mac = (String) keyIt.next();
			//System.out.println(c+" "+mac+"\t"+lnkMap.get(mac));
			//System.out.println(c+" "+mac+"\tvec2"+vec2.getRSSI(mac));
			/*for( String tmp : vec2.vec.keySet()){
				if( tmp == mac )
					System.out.println("!!!!!!!!"+mac+"="+tmp);
			}*/
			if( lnkMap.get(mac)==null ){
				//-100 RSSI for missing APs
				dist +=Math.sqrt( Math.abs( -100 - (Integer)lnkMap.get(mac) ));
				
			}
			else {
				dist += Math.sqrt( Math.abs( vec2.getRSSI(mac) - (Integer)lnkMap.get(mac) ));
				//System.out.println(mac+" "+vec2.getRSSI(mac)+" "+(Integer)lnkMap.get(mac));
			}
			c++;
		}
		
		/*for ( String mac_addr : vec1.vec.keySet() )
		{
			if( vec1.getRSSI(mac_addr) <-75 )
				continue;
			if( vec2.getRSSI(mac_addr)==0 )
				//-100 RSSI for missing APs
				dist +=Math.sqrt( Math.abs( -100 - vec1.getRSSI(mac_addr) ));
			else 
				dist += Math.sqrt( Math.abs( vec2.getRSSI(mac_addr) - vec1.getRSSI(mac_addr) ));
		}*/
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

