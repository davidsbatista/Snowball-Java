package clustering;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jblas.FloatMatrix;

import tuples.BREDSTuple;
import tuples.SnowballTuple;
import utils.Pair;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import bin.BREDSConfig;
import bin.SnowballConfig;

public class Singlepass {
	
	/*
	 * Cluster the Tuples objects according to:
	 * - TF-IDF representations
	 * - Word2Vec vector representation
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
		
	
	
	public static void SinglePassBREDS(LinkedList<BREDSTuple> tuples, List<BREDSPattern> patterns) {
		
		System.out.println(tuples.size() + " tuples to process");
		int count = 0;
		int start = 0;
		
		// Initialize: first tuple goes to first cluster
		if (patterns.size()==0) {
			BREDSPattern c1 = new BREDSPattern(tuples.get(0));
			patterns.add(c1);
			start = 1;
		}
		
		// Compute the similarity with each cluster
		for (int i = start; i < tuples.size(); i++) {
			
			double max_similarity = 0;
			int max_similarity_cluster_index = 0;
			if (count % 100 == 0) System.out.print(".");
			
			for (int j = 0; j < patterns.size(); j++) {
				
				BREDSPattern extractionPattern = patterns.get(j);				
				double similarity = 0;
				
				if (tuples.get(i).ReVerbpatterns.size()>0) {
					
					FloatMatrix patternVector = null;
					FloatMatrix sentence = tuples.get(i).relationalWordsVector.get(0);
					
					// represent the sentence as the use the sum of the relational words vectors				
					if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) {						
						patternVector = extractionPattern.sum();						
					}
					
					// represent the sentence as the centroid of the relational words vectors
					else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) {												
						patternVector = extractionPattern.centroid();						
					}
					
					// case when then extraction pattern is represented as single vector
					// similarity calculate with just one vector
					if (BREDSConfig.similarity.equalsIgnoreCase("single-vector")) {
						similarity = TermsVector.cosSimilarity(sentence, patternVector);
						
						/*
						System.out.println("relational words		 : " + relationalWords);
						System.out.println("pattern relational words : " + extractionPattern.patterns);						
						System.out.println("cosine 					 : " + similarity);
						System.out.println();
						*/
					}
					
					// all the vectors part of the extraction are used to calculate the similarity
					// with a sentence, compare all vectors individually using REDSPattern.all()
					// 	if the similarity with the majority is > threshold
					// 	returns a Pair, with true, and max_similarity score
					// 	otherwise returns False,0
					
					else if (BREDSConfig.similarity.equalsIgnoreCase("all")) {
						
						Pair<Boolean,Double> result = extractionPattern.all(sentence);
						
						if (result.getFirst()==true) similarity = result.getSecond();
						else similarity = 0.0;
						
						/*
						System.out.println("relational words		 : " + relationalWords);
						System.out.println("pattern relational words : " + extractionPattern.patterns);						
						System.out.println("similarity				 : " + similarity);
						System.out.println();
						*/
					}
					
					if (similarity > max_similarity) {					
						max_similarity = similarity;
						max_similarity_cluster_index = j;
					}
				}				
			}
				
			// If max_similarity < min_degree_match create a new cluster having this tuple as the centroid */			
			if ( max_similarity < BREDSConfig.threshold_similarity) {
				BREDSPattern c = new BREDSPattern(tuples.get(i));
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