package LocationEstimation;

/**
 * Location Estimation
 * including a spectrum of different methods
 */

import java.util.*;
import java.math.*;

import Data.*;
import State.Coordinate;

public class LocEstim
{
	public LocEstim()
	{
		// do nothing
	}
	
	
	// kNN estimation for the Signal data query against the current data set
	// return the meta grid number for the data set
	public int knn_estimate( SignalVector query, Vector<SignalVector> dataset, int k )
	{
		LinkedList<Double> distance = new LinkedList<Double>();
		LinkedList<SignalVector> candidate = new LinkedList<SignalVector>();
		
		// obtain the k candidates
		for ( int i=0; i<dataset.size(); ++i )
		{
			SignalVector curr_vec = dataset.elementAt(i);
			Double curr_dist = SignalVector.distEuclidean(query, curr_vec);
			int size = candidate.size(); 
			if ( size < k )
			{
				boolean flag = true;
				for ( int j=0; j<size; ++j )
				{
					if ( distance.get(j) > curr_dist )
					{
						flag = false;
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						break;
					}
				}
				if ( flag )
				{
					candidate.add(curr_vec);
					distance.add(curr_dist);
				}
			}
			else
			{
				for ( int j=0; j<size; ++j )
				{
					if ( distance.get(j) > curr_dist )
					{
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						candidate.removeLast();
						distance.removeLast();
						break;
					}
				}
			}	
		}
		
		// vote for a result in a uniform manner
		Map<Integer, Integer> vote_count = new HashMap<Integer, Integer>();
		for ( int i=0; i<candidate.size(); ++i )
		{
			int curr_meta = candidate.get(i).meta_grid;
			vote_count.put(curr_meta, 
				(vote_count.containsKey(curr_meta)? 
						vote_count.get(curr_meta)+1 : 1));
		}
		
		// display the result
		int majority = 0;
		int best_meta = -1;
		for ( Integer cand_meta : vote_count.keySet() )
		{
			int curr_vote = vote_count.get(cand_meta);
			if ( curr_vote > majority )
			{
				majority = curr_vote;
				best_meta = cand_meta;
			}
		}
		
		return best_meta;
	}

	
	public static void main( String[] args )
	{			
		// nothing here currently
		System.out.println("nothing here currently");
	}
}
