package LocationEstimation;

/**
 * Location Estimation
 * including a spectrum of different methods
 */

import java.util.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.*;

import Data.*;
import State.Coordinate;

public class LocEstim {

	private SignalVector rssiAverage;

	public LocEstim() {
		rssiAverage = new SignalVector();
	}

	// kNN estimation for the Signal data query against the current data set
	// return the meta grid number for the data set
	public int knn_estimate(SignalVector query, Vector<SignalVector> dataset,
			int k) {

		LinkedList<Double> distance = new LinkedList<Double>();
		LinkedList<SignalVector> candidate = new LinkedList<SignalVector>();
		System.out.println("dataset size = " + dataset.size());
		// obtain the k candidates
		for (int i = 0; i < dataset.size(); ++i) {
			SignalVector curr_vec = dataset.elementAt(i);
			Double curr_dist = SignalVector.distEuclidean(query, curr_vec);
			// System.out.println("Grid ="+curr_vec.meta_grid+" dist =" +
			// curr_dist);
			// System.out.println(i);
			int size = candidate.size();
			if (size < k) {
				boolean flag = true;
				for (int j = 0; j < size; ++j) {
					if (distance.get(j) > curr_dist) {
						flag = false;
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						// System.out.println("\t\t\t\tInsert : "+curr_vec.meta_grid+" "
						// +
						// "at position "+(j+1)+"[candidate size = "+
						// candidate.size()+"]");
						break;
					}
				}
				if (flag) {
					candidate.add(curr_vec);
					distance.add(curr_dist);
					// System.out.println("\t\t\t\tAdd : " +curr_vec.meta_grid
					// +" " +
					// "to position "+(size+1)+"[candidate size = "+
					// candidate.size()+"]");
				}
			} else {
				for (int j = 0; j < size; ++j) {
					if (distance.get(j) > curr_dist) {
						candidate.add(j, curr_vec);
						distance.add(j, curr_dist);
						candidate.removeLast();
						distance.removeLast();
						// System.out.println("\t\t\t\tReplace : "+curr_vec.meta_grid+" "
						// +
						// "to position "+ (j+1)+"[candidate size = "+
						// candidate.size()+"]");
						break;
					}
				}
			}
		}
		System.out.println("candiadate Size = " + candidate.size());
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

	// Select the best-grid by averaged dataset
	public int avg_estimate(SignalVector query, Vector<SignalVector> avgDataset) {
		int best_grid = -1;
		Double best_dist = 999.0;
		Double curr_dist = 999.0;
		for (int i = 0; i < avgDataset.size(); i++) {
			curr_dist = SignalVector.distEuclidean(query, avgDataset
					.elementAt(i));
			if (curr_dist < best_dist) {
				best_dist = curr_dist;
				best_grid = i + 1;
			}
		}
		return best_grid;
	}

	/**
	 * A naive approach for Bayesian Probability to find the most possible
	 * location estimation.
	 * 
	 * P(Grid=k| RSSI<set>) = P(RSSI<set>|Grid=k)P(Grid=k)/P(RSSI<set>)
	 * P(Grid=k) : probability of grid number equals k P(RSSI<set>) :
	 * probability of RSSI<set> occurs in database P(Grid=k| RSSI<set>) :
	 * probability of grid number equals k, given a RSSI<set>
	 * P(RSSI<set>|Grid=k) : probability of RRSI<set> occurs, given grid number
	 * k
	 * 
	 * The formula can further simplify to P(Grid=k| RSSI<set>) =
	 * P(RSSI<set>|Grid=k)*constant Using naive approach and assuming rssi
	 * signal independence, P(RSSI<set>|Grid=k) =
	 * P(AP1=RSSI1|Grid=k)*P(AP2=RSSI2|Grid=k)*....P(APi=RSSIi|Grid=k)
	 * 
	 * This estimation aims at finding max( P(Grid=k| RSSI<set> ) and return k
	 * 
	 * @param query
	 *            The observed RSSI set
	 * @param dataset
	 *            The organized dataset
	 * @return best_grid The estimated location
	 */
	public int bayesianEstimation(SignalVector query,
			Vector<Vector<SignalVector>> dataset) {
		int best_grid = -1;
		double best_prob = 0.0;
		double curr_prob = 0.0;
		HashMap<String, HashMap> profile = null;

		Vector<SignalVector> vsv = null;
		SignalVector sv = null;
		for (int i = 0; i < dataset.size(); i++) {
			System.out.println(">>>>>>" + (i + 1));
			profile = this.rssi_profile(i + 1, dataset);
			for (String key : query.vec.keySet()) {
				if (profile.containsKey(key)) {
					if (profile.get(key).containsKey(query.getRSSI(key))) {
						if (curr_prob == 0.0)
							curr_prob = 1.0;
						System.out.println(profile.get(key).get(
								query.getRSSI(key)));
						curr_prob *= (Double) (profile.get(key).get(query
								.getRSSI(key)));
					}
				}
			}
			// to compensate the missed APs, using average rssi over the whole
			// dataset
			for (String key : rssiAverage.vec.keySet()) {
				if (!query.vec.containsKey(key)) {
					if (profile.containsKey(key)) {
						if( profile.get(key).containsKey(rssiAverage.getRSSI(key))){
							if (curr_prob == 0.0)
								curr_prob = 1.0;
							curr_prob *= (Double) (profile.get(key)
									.get(rssiAverage.getRSSI(key)));
						}
						
					}
				}
			}
			System.out.println("****" + curr_prob);
			if (curr_prob > best_prob) {
				best_prob = curr_prob;
				best_grid = i + 1;
			}
			// reset to 0
			curr_prob = 0.0;
		}
		return best_grid;
	}

	/**
	 * To computer the RSSI probability profile of a grid
	 * 
	 * @param gridNum
	 *            The grid number of profile
	 * @param dataset
	 *            The training data
	 * @return profile The rssi profiles of the given grid number
	 */
	public static HashMap<String, HashMap> rssi_profile(Integer gridNum,
			Vector<Vector<SignalVector>> dataset) {
		double prob = 0.0;
		// HashMap holding the probability of a mac address in the given grid
		// number
		HashMap<String, HashMap> profile = new HashMap<String, HashMap>();
		// HashMap holding the rssi profile of a mac address in the given grid
		// number
		HashMap<String, HashMap> rssiProfile = new HashMap<String, HashMap>();
		// HashMap holding the probability of a given rssi
		HashMap<Integer, Double> probProfile;
		// HashMap holding the occurence of a given rssi
		HashMap<Integer, Integer> occurProfile;

		Vector<SignalVector> vsv = dataset.elementAt(gridNum - 1);
		SignalVector sv = null;
		Integer rssi = null;
		for (int i = 0; i < vsv.size(); i++) {
			sv = vsv.elementAt(i);

			for (String key : sv.vec.keySet()) {
				rssi = sv.getRSSI(key);
				if (rssiProfile.containsKey(key)) {
					occurProfile = rssiProfile.get(key);
					if (occurProfile.containsKey(rssi)) {
						occurProfile.put(rssi, occurProfile.get(rssi) + 1);
					} else {
						occurProfile.put(rssi, 1);
					}
				} else {
					occurProfile = new HashMap<Integer, Integer>();
					occurProfile.put(rssi, 1);
					rssiProfile.put(key, occurProfile);
				}
			}
		}

		// Computing probability
		for (String key : rssiProfile.keySet()) {
			// System.out.println(">>>>>>>"+key);
			occurProfile = rssiProfile.get(key);
			Integer total = 0;
			probProfile = new HashMap<Integer, Double>();
			for (Integer rssiKey : occurProfile.keySet()) {
				total += occurProfile.get(rssiKey);
			}
			for (Integer rssiKey : occurProfile.keySet()) {
				probProfile.put(rssiKey, (double) occurProfile.get(rssiKey)
						/ total);
				// System.out.println(rssiKey+" "+(double)occurProfile.get(rssiKey)/total);
			}
			profile.put(key, probProfile);
		}

		return profile;
	}

	/**
	 * Generate a RSSI's average file for Bayesian Probability Estimation.
	 */
	public void rssiAverage(Vector<Vector<SignalVector>> dataset) {
		// mapping mac address to accumulated rssi
		HashMap<String, Integer> rssiMap = new HashMap<String, Integer>();
		// mapping mac address to number of occurrence
		HashMap<String, Integer> countMap = new HashMap<String, Integer>();

		Vector<SignalVector> vsv = null;
		SignalVector sv = null;
		Integer counter = 0;
		for (int i = 0; i < dataset.size(); i++) {
			vsv = dataset.elementAt(i);
			for (int j = 0; j < vsv.size(); j++) {
				sv = vsv.elementAt(j);
				for (String key : sv.getMacAddr()) {
					if (rssiMap.containsKey(key)) {
						rssiMap.put(key, rssiMap.get(key) + sv.getRSSI(key));
						countMap.put(key, countMap.get(key) + 1);
					} else {
						rssiMap.put(key, sv.getRSSI(key));
						countMap.put(key, 1);
						counter++;
					}
				}
			}
		}
		// perform averaging
		for (String key : rssiMap.keySet()) {
			rssiAverage.put(key, rssiMap.get(key) / countMap.get(key));
		}

		PrintWriter fout = null;
		try {
			fout = new PrintWriter("src/dataset/averageRSS");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		fout.println("% Number of APs : " + counter);
		fout.println("% Format : #mac address #average rssi\n");
		for (String key : rssiMap.keySet()) {
			if( key.equals("11"))
				System.out.println("what!!!!!!!!!!!!!");
			fout.println(key + " " + rssiMap.get(key) / countMap.get(key));
		}
		fout.close();

	}

	public static void main(String[] args) {
		// nothing here currently
		System.out.println("Testing location estimation");

		LocEstim locEst = new LocEstim();
		SignalDatabase db = new SignalDatabase();
		String signal = "src/dataset/VirtualSignal";
		Vector<SignalStrength> ss = new Vector<SignalStrength>();
		SignalVector sv = new SignalVector();

		db.loadDataSet();

		Scanner fin = null;
		try {
			fin = new Scanner(new FileInputStream(signal));
		} catch (FileNotFoundException e) {
			System.err.println("loading signal data error: the file " + signal
					+ " is not found.");
			System.err.println(e.toString());
			return;
		}

		Vector<String> lines = new Vector<String>();
		String mac, name;
		int rssi, x, y;
		while (fin.hasNextLine()) {
			lines.add(fin.nextLine().trim());
		}

		for (int i = 0; i < lines.size(); i++) {
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
		// int bestGrid = locEst.knn_estimate(sv, db.sig_vec_base, 5);
		int bestGrid2 = locEst.knn_estimate_dataset2(sv, db.sig_vec_base_v, 5);
		// int bestGrid3 = locEst.avg_estimate(sv, db.sig_vec_base_avg);
		// System.out.println("Best Grid ="+bestGrid +
		// "\tNew Best Grid ="+bestGrid2 +"\tAvg Best Grid ="+bestGrid3);
		locEst.rssiAverage(db.sig_vec_base_v);

		// testing rssi profiles computation
		HashMap<String, HashMap> rssiProfile = locEst.rssi_profile(1,
				db.sig_vec_base_v);
		int bestGrid4 = locEst.bayesianEstimation(sv, db.sig_vec_base_v);
		System.out.println("bestGrid : " + bestGrid2 + " " + bestGrid4);
	}
}
