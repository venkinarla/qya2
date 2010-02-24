package LocationEstimation;

/**
 * Location Estimation
 * including a spectrum of different methods
 */

import java.util.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.*;

import Data.*;
import State.Coordinate;

public class LocEstim {
	public LocEstim() {
		// do nothing
	}

	// kNN estimation for the Signal data query against the current data set
	// return the meta grid number for the data set
	public int knn_estimate(SignalVector query, Vector<SignalVector> dataset,
			int k) {
		
		
		LinkedList<Double> distance = new LinkedList<Double>();
		LinkedList<SignalVector> candidate = new LinkedList<SignalVector>();
		System.out.println("dataset size = "+dataset.size());
		// obtain the k candidates
		for (int i = 0; i < dataset.size(); ++i) {
			SignalVector curr_vec = dataset.elementAt(i);
			Double curr_dist = SignalVector.distEuclidean(query, curr_vec);
			System.out.println("Grid ="+curr_vec.meta_grid+" dist =" + curr_dist);
			int size = candidate.size();
			if (size < k) {
				boolean flag = true;
				for (int j = 0; j < size; ++j) {
					if (distance.get(j) > curr_dist) {
						flag = false;
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						System.out.println("\t\t\t\tInsert : "+curr_vec.meta_grid+" " +
								"at position "+(j+1)+"[candidate size = "+ candidate.size()+"]");
						break;
					}
				}
				if (flag) {
					candidate.add(curr_vec);
					distance.add(curr_dist);
					System.out.println("\t\t\t\tAdd : " +curr_vec.meta_grid +" " +
								"to position "+(size+1)+"[candidate size = "+ candidate.size()+"]");
				}
			} else {
				for (int j = 0; j < size; ++j) {
					if (distance.get(j) > curr_dist) {
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						candidate.removeLast();
						distance.removeLast();
						System.out.println("\t\t\t\tReplace : "+curr_vec.meta_grid+" " +
								"to position "+ (j+1)+"[candidate size = "+ candidate.size()+"]");
						break;
					}
				}
			}
		}
		System.out.println("candiadate Size = "+candidate.size());
		// vote for a result in a uniform manner
		Map<Integer, Integer> vote_count = new HashMap<Integer, Integer>();
		for (int i = 0; i < candidate.size(); ++i) {
			int curr_meta = candidate.get(i).meta_grid;
			vote_count.put(curr_meta,
					(vote_count.containsKey(curr_meta) ? vote_count
							.get(curr_meta) + 1 : 1));
		}

		// display the result
		int majority = 0;
		int best_meta = -1;
		for (Integer cand_meta : vote_count.keySet()) {
			int curr_vote = vote_count.get(cand_meta);
			if (curr_vote > majority) {
				majority = curr_vote;
				best_meta = cand_meta;
			}
		}

		return best_meta;
	}

	// kNN estimation for the Signal data query against the current data set
	// return the meta grid number for the data set
	public int knn_estimate_dataset2(SignalVector query,
			Vector<Vector<SignalVector>> dataset, int k) {
		LinkedList<Double> distance = new LinkedList<Double>();
		LinkedList<SignalVector> candidate = new LinkedList<SignalVector>();

		// obtain the k candidates
		for (int i = 0; i < dataset.size(); ++i) {
			Vector<SignalVector> curr_vec_vec = dataset.elementAt(i);
			for (int j = 0; j < curr_vec_vec.size(); j++) {
				Double curr_dist = SignalVector.distEuclidean(query,
						curr_vec_vec.elementAt(j));
				int size = candidate.size();
				if (size < k) {
					boolean flag = true;
					for (int m = 0; m < size; ++m) {
						if (distance.get(m) > curr_dist) {
							flag = false;
							candidate.add(m, curr_vec_vec.elementAt(j));
							distance.add(m, curr_dist);
							break;
						}
					}
					if (flag) {
						candidate.add(curr_vec_vec.elementAt(j));
						distance.add(curr_dist);
					}
				} else {
					for (int m = 0; m < size; ++m) {
						if (distance.get(m) > curr_dist) {
							candidate.add(m, curr_vec_vec.elementAt(j));
							distance.add(m, curr_dist);
							candidate.removeLast();
							distance.removeLast();
							break;
						}
					}
				}
			}
		}

		// vote for a result in a uniform manner
		Map<Integer, Integer> vote_count = new HashMap<Integer, Integer>();
		for (int i = 0; i < candidate.size(); ++i) {
			int curr_meta = candidate.get(i).meta_grid;
			vote_count.put(curr_meta,
					(vote_count.containsKey(curr_meta) ? vote_count
							.get(curr_meta) + 1 : 1));
		}

		// display the result
		int majority = 0;
		int best_meta = -1;
		for (Integer cand_meta : vote_count.keySet()) {
			int curr_vote = vote_count.get(cand_meta);
			if (curr_vote > majority) {
				majority = curr_vote;
				best_meta = cand_meta;
			}
		}

		return best_meta;
	}
	
	//utility
	public void printArray(){
		
	}
	public static void main( String[] args )
	{			
		// nothing here currently
		System.out.println("Testing location estimation");
		
		LocEstim locEst = new LocEstim();
		SignalDatabase db = new SignalDatabase();
		String signal = "src/dataset/VirtualSignal";
		Vector<SignalStrength> ss = new Vector<SignalStrength>();
		SignalVector sv =  new SignalVector();
		
		db.loadDataSet();
		
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
		System.out.println("Estimating...");
		int bestGrid = locEst.knn_estimate(sv, db.sig_vec_base, 5);
		System.out.println("Best Grid ="+bestGrid);
		
	}
}
