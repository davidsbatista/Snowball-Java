package bin;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jblas.FloatMatrix;

import tuples.Seed;
import tuples.Tuple;
import utils.Pair;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import clustering.SnowballPattern;
import clustering.dbscan.CosineMeasure;

public class REDS {

	public static int iter = 0;
	
	public static void start(String sentencesFile, String seedsFile,Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException, Exception {		
		long startTime = System.nanoTime();
		Config.readSeeds(seedsFile);
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	static void iteration(long startTime, String sentencesFile, Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException, Exception {					
		
		System.out.println("Pre-processing data...");
		Set<Tuple> processedTuples = new HashSet<Tuple>();
		Snowball.generateTuples(sentencesFile,processedTuples);
		
		System.out.println("\n"+processedTuples.size() + " tuples gathered");
				
		while (iter<=Config.parameters.get("number_iterations")) {
			
			// Collect sentences (Tuple objects) where both entities occur 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Seed matches:\n");
			//LinkedList<Tuple> seedMatches = matchSeedsTuples(processedTuples);			
			LinkedList<Tuple> seedMatches = Snowball.matchSeedsTuples(processedTuples);
			if (seedMatches.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			else {
				if (iter==0) {
					DBSCAN(seedMatches,patterns);
					System.out.println("\n"+patterns.size() + " patterns generated");					
					if (patterns.size()==0) {
						System.out.println("No patterns generated");
						System.exit(0);
					}
				}
				else {
					// Calculate similarity with each pattern 
					// If Sim > 0.8 with a pattern from a cluster 
					// Tuple becomes part of that cluster
					int added = 0;
					ListIterator<Tuple> iter = seedMatches.listIterator();
					while ( iter.hasNext() ) {
						Tuple tuple = iter.next();		
						for (SnowballPattern p : patterns) {
		    				
		    				// Compare the ReVerb patterns/middle words from the sentence 
		    				// with every Tuple part of a Pattern
		    				
		    				/*
		    				// Just takes the maximum value
		    				double bestScore = 0;
		    				for (String w : p.patterns) {
								String[] tokens = w.split("\\s");
								FloatMatrix a = CreateWord2VecVectors.createVecSum(Arrays.asList(tokens));																		
								FloatMatrix b = tuple.middleReverbPatternsWord2VecSum.get(0);
								double score = TermsVector.cosSimilarity(a, b);
								if (score>=0.8) {
									if (score>bestScore) {
										bestScore = score;
									}
								}
							}
		    				if (bestScore>=0.8) {
		    					p.addTuple(tuple);
		    					p.mergUniquePatterns();
		    					iter.remove();
		    					added++;
		    				}
		    				*/
		    				
		    				// If the majority is >0.8
		    				int good = 0;
		    				int bad = 0;
		    				for (List<String> patternTokens : p.patterns) {
								FloatMatrix a = CreateWord2VecVectors.createVecSum(patternTokens);																		
								FloatMatrix b = tuple.middleReverbPatternsWord2VecSum.get(0);
								double score = TermsVector.cosSimilarity(a, b);
								if (score>=0.8) good++;
								else bad++;
								
							}
		    				if (good>bad) {
		    					p.addTuple(tuple);
		    					p.mergUniquePatterns();
		    					iter.remove();
		    					added++;
		    				}
						}
					}
					
					// For tuples that don't belong to any existing cluster
					// discard					
					System.out.println("\nTuples added to clusters:");
					System.out.println(added);
					System.out.println("\nTuples with low similarity with clusters (discarded)");
					System.out.println(seedMatches.size());
					
					/*
					for (Tuple tuple : seedMatches) {
						System.out.println(tuple.sentence);
						if (tuple.hasReVerbPatterns==false) {
							String[] tokens = tuple.tagged_middle_text.getFirst();
							String[] pos = tuple.tagged_middle_text.getSecond();
							System.out.println(Arrays.asList(tokens));  
							System.out.println(Arrays.asList(pos));	
						}
						else {
							System.out.println(tuple.ReVerbpatterns);	
						}						
						System.out.println();
					}
					LinkedList<SnowballPattern> newPatterns = new LinkedList<SnowballPattern>(); 
					DBSCAN(seedMatches,newPatterns);
					
					System.out.println("\nGenerated patterns");
					for (SnowballPattern p: newPatterns) {
						p.confidence();
						System.out.println("#tuples		:" + p.tuples.size());
						System.out.println(p.patterns);						
						System.out.println("====================================\n");
					}
					*/
					
				}
								
				System.out.println();
				
				//Expand extraction patterns with similar words
				//TODO: expandPatterns(patterns);				
				
				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence(Tuple) with each Pattern
				// - Matching Tuple objects are used to score a Pattern confidence, based on being correct
				//   or not according to the seed set				
				System.out.println("Computing similarity of " + Config.e1_type + " - " + Config.e2_type + " tuples with patterns");								
				comparePatternsTuples(candidateTuples, patterns, processedTuples);				
				System.out.println("\n"+candidateTuples.size() + " tuples found");				
				
				/*
				for (Tuple tuple : candidateTuples.keySet()) {
					System.out.println(tuple.sentence);
					System.out.println(tuple.e1 + '\t' + tuple.e2);						
					System.out.println("Matched with " + candidateTuples.get(tuple).size() + " patterns");						
					int count = 0;
					for (Pair<SnowballPattern, Double> match : candidateTuples.get(tuple)) {
						System.out.println("Cluster " + count + ' ' + match.getSecond());
						System.out.println(match.getFirst().patterns);
						count++;
					}
					System.out.println();
				}
				*/
				
				System.out.println("\nPatterns confidence updated");
				for (SnowballPattern p: patterns) {
					p.confidence();
					System.out.println("confidence	:" + p.confidence);
					System.out.println("#tuples		:" + p.tuples.size());
					System.out.println(p.patterns);						
					System.out.println("====================================\n");
				}
				
				// Update Tuple confidence based on patterns confidence
				System.out.println("Calculating tuples confidence");
				Snowball.calculateTupleConfidence(candidateTuples);

				// Print each collected Tuple and its confidence
				/*
				ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (Tuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
				*/
									
				// Calculate a new seed set of tuples to use in next iteration, such that:
				// seeds = { T | Conf(T) > min_tuple_confidence }
				System.out.println("Adding tuples with confidence =>" + Config.parameters.get("min_tuple_confidence") + " as seed for next iteration");
				int added = 0;
				int removed = 0;				
				for (Tuple t : candidateTuples.keySet()) {
					if (t.confidence>=Config.parameters.get("min_tuple_confidence")) {
						Config.seedTuples.add(new Seed(t.e1.trim(),t.e2.trim()));
						added++;
					} else removed++;
				}
				System.out.println(removed + " tuples removed due to confidence lower than " + Config.parameters.get("min_tuple_confidence"));				
				System.out.println(added + " tuples added to seed set");
				iter++;
			}			
		}
		
		long stopTime = System.nanoTime();
		long elapsedTime = stopTime - startTime;		
		long elapsedTimeSeconds = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);		
		long hours = elapsedTimeSeconds / 3600;
		long minutes = (elapsedTimeSeconds % 3600) / 60;
		long seconds = elapsedTimeSeconds % 60;
		String timeString = hours + ":" + minutes + ":" + seconds + " seconds";
		System.out.println("Runtime: " + timeString);		
		System.out.println();			
		System.out.println(candidateTuples.size() + " tuples extracted");			
	}
	
	
	/*
	 * Clusters all the collected Tuples with DBSCAN
	 *  - ReVerb patterns are extracted from the middle context
	 *  - If no patterns are found, the middle words are considered
	 *  - Word2Vec vectors from the ReVerb patterns/middle words are summed
	 *  - Tuples are clustered according to: 1-cos(a,b) where 'a' and' b' are Word2Vec vectors
	 */  
	private static void DBSCAN(LinkedList<Tuple> tuples, LinkedList<SnowballPattern> patterns) {
		DistanceMeasure measure = new CosineMeasure();
		double eps = 1-Config.parameters.get("min_tuple_confidence");
		int minPts = 2;
		DBSCANClusterer<Clusterable> dbscan = new DBSCANClusterer<>(eps, minPts, measure);

		// Tuples implement the Clusterable Interface to allow clustering
		// Add all tuples to a Clusterable collection to be passed to DBSCAN
		LinkedList<Clusterable> points = new LinkedList<Clusterable>();
				
		for (Tuple t : tuples) {			
			// Cluster according to ReVerb patterns
			// TODO: tuples com mais do que um padrão ReVerb ?
			if (t.middleReverbPatternsWord2VecSum.size()>0) {
				points.add(t);
			}	
		}
		
		/* Cluster ReVerb patterns/words in middle context */		
		int not_considered = tuples.size()-points.size();
		System.out.println();
		System.out.println("#Tuples to cluster	  : " + points.size());
		System.out.println("#Tuples not considered : " + not_considered);		
		List<Cluster<Clusterable>> clusters = dbscan.cluster(points);		
		System.out.println("\nClusters generated: " + clusters.size());
		
		/* Transform the clustered tuples into SnowballPattern */
		int c = 1;
		for (Cluster<Clusterable> cluster : clusters) {
			List<Clusterable> objects = cluster.getPoints();
			SnowballPattern pattern = new SnowballPattern();
			System.out.println("Cluster " + c);
			for (Clusterable object : objects) {
				Tuple t = (Tuple) object;
				pattern.tuples.add(t);
			}			
			patterns.add(pattern);
			pattern.mergUniquePatterns();
			System.out.println(pattern.patterns);
			System.out.println();
			c++;
		}
	}
	
	
	static void comparePatternsTuples(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, Set<Tuple> processedTuples) {
		
		for (Tuple tuple : processedTuples) {
			// Compute similarity with all the extraction patterns			
			if (tuple.ReVerbpatterns.size()>0) {    			
    			for (SnowballPattern p : patterns) {
    				double bestScore = 0;
    				// Compare the ReVerb patterns/middle words from the sentence 
    				// with every extraction pattern from the clusters		        				
    				for (List<String> patternTokens : p.patterns) {						
						FloatMatrix a = CreateWord2VecVectors.createVecSum(patternTokens);																		
						FloatMatrix b = tuple.middleReverbPatternsWord2VecSum.get(0);
						double score = TermsVector.cosSimilarity(a, b);
						if (score>=0.8) {
							if (score>bestScore) {
								bestScore = score;
							}
						}
					}
    				if (bestScore>=0.8) {
    					Pair<SnowballPattern, Double> matched = new Pair<SnowballPattern, Double>(p, bestScore);
    					if (candidateTuples.containsKey(tuple)) {
							List<Pair<SnowballPattern, Double>> matches = candidateTuples.get(tuple);										
							matches.add(matched);
						}		        					
    					else {		        						
    						LinkedList<Pair<SnowballPattern, Double>> matchedPatterns = new LinkedList<>();
    						matchedPatterns.add(matched);
    						candidateTuples.put(tuple,matchedPatterns);
        				}
    					p.updatePatternSelectivity(tuple.e1, tuple.e2);
    				}
    			}
			}
		}
	}
	
	/*
	 * Expands the relationship words of a pattern based on similarities
	 * 	- For each word part of a pattern
	 * 		- Construct a set with all the similar words according to Word2Vec given a threshold t    	 
	 *  	- Calculate the intersection of all sets
	 */
	private static void expandPatterns(LinkedList<SnowballPattern> patterns) {
		for (SnowballPattern p : patterns) {
			/*
				Set<WordEntry> words = Config.word2vec.distance(word);
				for (WordEntry wordEntry : words) {
					System.out.println(wordEntry.name + '\t' + wordEntry.score);
			}
			*/
			/* TESTAR
			List<String> pattern_words = new LinkedList<String>();
			pattern_words.addAll(t.middle_words);
			Set<WordEntry> words = Config.word2vec.distance(pattern_words);
			*/
		}					
	}
}




