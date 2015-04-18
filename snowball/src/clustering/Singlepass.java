package clustering;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import tuples.SnowballTuple;
import bin.SnowballConfig;

public class Singlepass {
	
	/*
	 * Cluster the Tuples objects according to TF-IDF representations 
	 */ 		
				
	public static void singlePassTFIDF(LinkedList<SnowballTuple> tuples, List<SnowballPattern> patterns) throws IOException {
		System.out.println(tuples.size() + " tuples to process");		
		int count = 0;
				
		// Initialize: first tuple is first cluster
		if (patterns.size()==0) {
			SnowballPattern c1 = new SnowballPattern(tuples.get(0));
			patterns.add(c1);
		}
				
		/* Go through all tuples and calculate the 
		 * similarity with each cluster centroid  
		 */  		
		for (int i = 1; i < tuples.size(); i++) {
			double max_similarity = 0;
			int max_similarity_cluster_index = 0;
			if (count % 100 == 0) System.out.print(".");			
			for (int j = 0; j < patterns.size(); j++) {				
				SnowballPattern c = patterns.get(j);				
				double similarity = tuples.get(i).degreeMatchCosTFIDF(c.left_centroid, c.middle_centroid, c.right_centroid);
				if (similarity>max_similarity) {					
					max_similarity = similarity;
					max_similarity_cluster_index = j;
				}
			}
			
			// if max_similarity < min_degree_match create new cluster/patterns having this tuple as the centroid */			
			if ( max_similarity<SnowballConfig.min_degree_match ) {
				SnowballPattern c = new SnowballPattern(tuples.get(i));
				patterns.add(c);
			}
			// if max_similarity >= min_degree_match add to pattern to the cluster and recalculate centroid */ 			
			else {				
				patterns.get(max_similarity_cluster_index).addTuple(tuples.get(i));
				patterns.get(max_similarity_cluster_index).calculateCentroidTFIDF("left");
				patterns.get(max_similarity_cluster_index).calculateCentroidTFIDF("middle");
				patterns.get(max_similarity_cluster_index).calculateCentroidTFIDF("right");
			}
		count++;
		}
		System.out.println();		
	}
}