package clustering;

import java.io.IOException;
import java.util.LinkedList;

import tuples.Tuple;
import bin.Config;

public class Singlepass {
	
	/*
	 * Cluster the Tuples objects according:
	 * to TF-IDF representations
	 * or Word2Vec vector representation
	 */ 
	public static void singlePass(LinkedList<Tuple> tuples, LinkedList<SnowballPattern> patterns) throws IOException {
		/*
		if (Config.useWord2Vec==true) {
			Singlepass.singlePassWord2Vec(tuples, patterns);
		}		
		else {
		*/
		Singlepass.singlePassTFIDF(tuples, patterns);			
	}			
				
	public static void singlePassTFIDF(LinkedList<Tuple> tuples, LinkedList<SnowballPattern> patterns) throws IOException {
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
			if ( max_similarity<Config.min_degree_match ) {
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
	
	
	public static void singlePassWord2Vec(LinkedList<Tuple> tuples, LinkedList<SnowballPattern> patterns) throws IOException {
		
		System.out.println(tuples.size() + " tuples to process");		
		int count = 0;
		int start = 0;
		
		// Initialize: first tuple is first cluster
		if (patterns.size()==0) {
			SnowballPattern c1 = new SnowballPattern(tuples.get(0));
			patterns.add(c1);
			start = 1;
		}		
		
		// Compute the similarity with each cluster centroid*/
		for (int i = start; i < tuples.size(); i++) {
			double max_similarity = 0;
			int max_similarity_cluster_index = 0;
			if (count % 100 == 0) System.out.print(".");			
			for (int j = 0; j < patterns.size(); j++) {				
				SnowballPattern c = patterns.get(j);
				double similarity = 0;
				if (Config.useSum==true) {
					similarity = tuples.get(i).degreeMatchWord2VecSum(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				else if (Config.useCentroid==true) {
					similarity = tuples.get(i).degreeMatchWord2VecCentroid(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				if (similarity > max_similarity) {					
					max_similarity = similarity;
					max_similarity_cluster_index = j;
				}
			}
			
			// If max_similarity < min_degree_match create a new cluster having this tuple as the Centroid */			
			if ( max_similarity < Config.min_degree_match ) {
				SnowballPattern c = new SnowballPattern(tuples.get(i));
				patterns.add(c);
			}
			
			// If max_similarity >= min_degree_match add to the cluster and recalculate centroid */ 			
			else {				
				patterns.get(max_similarity_cluster_index).addTuple(tuples.get(i));
				patterns.get(max_similarity_cluster_index).calculateCentroidWord2Vec();
			}
		count++;
		}		
	}



	public static void singlePassREDS(LinkedList<Tuple> seedMatches,LinkedList<SnowballPattern> patterns) {
		
		System.out.println(seedMatches.size() + " tuples to process");		
		int count = 0;
		int start = 0;
		
		// Initialize: first tuple is first cluster
		if (patterns.size()==0) {
			SnowballPattern c1 = new SnowballPattern(seedMatches.get(0));
			patterns.add(c1);
			start = 1;
		}
		
		// Compute the similarity with each cluster centroid*/
		for (int i = start; i < seedMatches.size(); i++) {
			double max_similarity = 0;
			int max_similarity_cluster_index = 0;
			if (count % 100 == 0) System.out.print(".");			
			for (int j = 0; j < patterns.size(); j++) {				
				SnowballPattern c = patterns.get(j);
				double similarity = 0;
				if (Config.useSum==true) {
					similarity = seedMatches.get(i).degreeMatchWord2VecSum(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				else if (Config.useCentroid==true) {
					similarity = seedMatches.get(i).degreeMatchWord2VecCentroid(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				if (similarity > max_similarity) {					
					max_similarity = similarity;
					max_similarity_cluster_index = j;
				}
			}
			
			// If max_similarity < min_degree_match create a new cluster having this tuple as the Centroid */			
			if ( max_similarity < Config.min_degree_match ) {
				SnowballPattern c = new SnowballPattern(seedMatches.get(i));
				patterns.add(c);
			}
			
			// If max_similarity >= min_degree_match add to the cluster and recalculate centroid */ 			
			else {				
				patterns.get(max_similarity_cluster_index).addTuple(seedMatches.get(i));
				patterns.get(max_similarity_cluster_index).calculateCentroidWord2Vec();
			}
		count++;
		}	
	}
}
