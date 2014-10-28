package clustering;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jblas.FloatMatrix;

import tuples.REDSTuple;
import tuples.Tuple;
import utils.Pair;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import bin.REDSConfig;
import bin.SnowballConfig;

public class Singlepass {
	
	/*
	 * Cluster the Tuples objects according to:
	 * - TF-IDF representations
	 * - Word2Vec vector representation
	 */ 		
				
	public static void singlePassTFIDF(LinkedList<Tuple> tuples, List<SnowballPattern> patterns) throws IOException {
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
		
	public static void singlePassWord2Vec(LinkedList<Tuple> tuples, List<SnowballPattern> patterns) throws IOException {
		
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
				if (SnowballConfig.useSum==true) {
					similarity = tuples.get(i).degreeMatchWord2VecSum(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				else if (SnowballConfig.useCentroid==true) {
					similarity = tuples.get(i).degreeMatchWord2VecCentroid(c.w2v_left_centroid, c.w2v_middle_centroid, c.w2v_right_centroid);
				}
				if (similarity > max_similarity) {					
					max_similarity = similarity;
					max_similarity_cluster_index = j;
				}
			}
		
			// If max_similarity < min_degree_match create a new cluster having this tuple as the Centroid */			
			if ( max_similarity < SnowballConfig.min_degree_match ) {
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
	
	public static void SinglePassREDS(LinkedList<REDSTuple> tuples, List<REDSPattern> patterns) {
		
		System.out.println(tuples.size() + " tuples to process");
		int count = 0;
		int start = 0;
		
		// Initialize: first tuple goes to first cluster
		if (patterns.size()==0) {
			REDSPattern c1 = new REDSPattern(tuples.get(0));
			patterns.add(c1);
			start = 1;
		}
		
		// Compute the similarity with each cluster
		for (int i = start; i < tuples.size(); i++) {
			
			double max_similarity = 0;
			int max_similarity_cluster_index = 0;
			if (count % 100 == 0) System.out.print(".");
			
			for (int j = 0; j < patterns.size(); j++) {
				
				REDSPattern extractionPattern = patterns.get(j);				
				double similarity = 0;
				
				if (tuples.get(i).ReVerbpatterns.size()>0) {
					List<String> relationalWords = tuples.get(i).ReVerbpatterns.get(0).token_words;
					
					if (REDSConfig.single_vector.equalsIgnoreCase("sum") || REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
						
						// use the sum of the relational words vectors of the instances in the pattern					
						if (REDSConfig.single_vector.equalsIgnoreCase("sum")) {						
							FloatMatrix patternVector = extractionPattern.centroid();
							FloatMatrix sentence = CreateWord2VecVectors.createVecSum(relationalWords); 						
							similarity = TermsVector.cosSimilarity(sentence, patternVector);
						}
						// use the centroid of the relational words vectors of the instances in the pattern
						else if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {												
							FloatMatrix patternVector = extractionPattern.sum();
							FloatMatrix sentence = CreateWord2VecVectors.createVecCentroid(relationalWords); 						
							similarity = TermsVector.cosSimilarity(sentence, patternVector);
						}
						
						if (similarity > max_similarity) {					
							max_similarity = similarity;
							max_similarity_cluster_index = j;
						}
					}
					
					// compare all vectors individually using REDSPattern.all()
					// if the similarity with the majority is > threshold
					// returns a Pair, with true, and max_similarity score
					// otherwise returns False,0
					else if (REDSConfig.single_vector.equalsIgnoreCase("all")) {
						FloatMatrix sentence = null;
						if (REDSConfig.single_vector.equalsIgnoreCase("sum")) {						
							sentence = CreateWord2VecVectors.createVecSum(relationalWords); 						
						}					
						else if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
							sentence = CreateWord2VecVectors.createVecCentroid(relationalWords);
						}
						Pair<Boolean,Double> result = REDSPattern.all(sentence);
												
						if (result.getFirst()==true) {
							max_similarity = result.getSecond();
							max_similarity_cluster_index = j;
						}
						else {
							max_similarity = result.getSecond();
						}
					}
				}				
			}
				
			// If max_similarity < min_degree_match create a new cluster having this tuple as the centroid */			
			if ( max_similarity < REDSConfig.similarity_threshold) {
				REDSPattern c = new REDSPattern(tuples.get(i));
				patterns.add(c);				
			}
			
			// If max_similarity >= min_degree_match add to the cluster and recalculate centroid */ 			
			else {				
				patterns.get(max_similarity_cluster_index).addTuple(tuples.get(i));
			}
			count++;	
		}		
	}
}