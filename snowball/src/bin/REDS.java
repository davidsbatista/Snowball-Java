package bin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import log.MyFormatter;
import nlp.ReVerbPattern;
import nlp.Stopwords;

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
import word2vec.com.ansj.vec.domain.WordEntry;
import clustering.Singlepass;
import clustering.SnowballPattern;
import clustering.dbscan.CosineMeasure;

public class REDS {

	//private static final Logger logger1 = Logger.getLogger( REDS.class.getName() );	
	//private static final Logger logger2 = Logger.getLogger( REDS.class.getName() );
	public static int iter = 0;
		
	public static void start(String sentencesFile, String seedsFile,Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException, Exception {		
		
		/*
		// Suppress the logging output to the console	    
	    logger1.setUseParentHandlers(false);
	    logger2.setUseParentHandlers(false);
	    	    
		FileHandler discarded_clustering = new FileHandler("tuples_discarded_clustering.log", false);
		FileHandler discarded_matching = new FileHandler("tuples_discarded_matchinng.log", false);
		discarded_clustering.setFormatter(new MyFormatter());
		discarded_matching.setFormatter(new MyFormatter());
		logger1.addHandler(discarded_clustering);
		logger2.addHandler(discarded_matching);
		*/
		
		// Start timing and extraction
		Config.readSeeds(seedsFile);
		long startTime = System.nanoTime();		
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	static void iteration(long startTime, String sentencesFile, Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException, Exception {					
		
		List<Tuple> processedTuples = new LinkedList<Tuple>();
		File f = new File("REDS_processed_tuples.obj");
		
		if (!f.exists()) {
			System.out.println("\nPre-processing data");
			Snowball.generateTuples(sentencesFile,processedTuples);
			System.out.println("\n"+processedTuples.size() + " tuples gathered");
			
			try {
				// Save to disk
				FileOutputStream out = new FileOutputStream("REDS_processed_tuples.obj");
				ObjectOutputStream oo = new ObjectOutputStream(out);
				oo.writeObject(processedTuples);
				oo.close();
			} catch (Exception e) { 
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		else {
			// Load
			System.out.println("Loading pre-processed sentences");
			FileInputStream in = new FileInputStream("REDS_processed_tuples.obj");
			ObjectInputStream objectInput = new ObjectInputStream(in);
			processedTuples = (List<Tuple>) objectInput.readObject();
			System.out.println("\n"+processedTuples.size() + " tuples gathered");						
			in.close();
		}
		
		while (iter<=Config.number_iterations) {
			// Collect sentences (Tuple objects) where both entities occur 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Seed matches:\n");			
			LinkedList<Tuple> seedMatches = Snowball.matchSeedsTuples(processedTuples);
			
			/*
			System.out.println("Seed matches: " + seedMatches.size());
			for (Tuple tuple : seedMatches) {
				System.out.println(tuple.sentence);
				for (ReVerbPattern rvb : tuple.ReVerbpatterns) {
					System.out.println(rvb.token_words);
				}
				System.out.println();
			}
			System.out.println();
			*/
						
			if (seedMatches.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			else {
				//TODO: melhorar o algoritm de clustering: fazer single-pass, ou usar sempre o DBSCAN em tudo				
				//if (iter==0) {
					//DBSCAN(seedMatches,patterns);
					Singlepass.SinglePassReVerb(seedMatches,patterns);
					System.out.println("\n"+patterns.size() + " patterns generated");					
					if (patterns.size()==0) {
						System.out.println("No patterns generated");
						System.exit(0);
					}
					
			        // Eliminate patterns supported by less than 'min_pattern_support' tuples			
					Iterator<SnowballPattern> patternIter = patterns.iterator();
					while (patternIter.hasNext()) {
						SnowballPattern p = patternIter.next();
						if (p.tuples.size()<Config.min_pattern_support) patternIter.remove();
					}
					patternIter = null;
					System.out.println(patterns.size() + " patterns supported by at least " + Config.min_pattern_support + " tuple(s)");
					//TODO: Dump initial patterns					
					// Compare with all learned patterns
					// Calculate how many different relational/words pattern were learned
					// How many extracted valid patterns
					
				//}
				/*
				else {
					// Calculate similarity with each pattern 
					// If similarity > min_degree_match with a pattern from a cluster 
					// Tuple becomes part of that cluster
					ListIterator<Tuple> iter = seedMatches.listIterator();
					while ( iter.hasNext() ) {
						Tuple tuple = iter.next();
						if (tuple.ReVerbpatterns.size()>=1) {
							for (SnowballPattern p : patterns) {
			    				// Compare the ReVerb patterns/middle words from the sentence 
			    				// with every Tuple part of a Pattern								
			    				// If the similarity with the majority is > threshold, add it
			    				int good = 0;
			    				int bad = 0;
			    				for (List<String> patternTokens : p.patterns) {			    					
									FloatMatrix a = CreateWord2VecVectors.createVecSum(patternTokens);																		
									FloatMatrix b = CreateWord2VecVectors.createVecSum(tuple.ReVerbpatterns.get(0).token_words);
									double score = TermsVector.cosSimilarity(a, b);
									
									System.out.println("pattern	: " + patternTokens);
									System.out.println("tuple	: " + tuple.ReVerbpatterns.get(0).token_words);
									System.out.println("score	: " + score);
									System.out.println();
									
									if (score>=Config.min_degree_match) good++;
									else bad++;
									
								}
			    				if (good>bad) {
			    					p.addTuple(tuple);
			    					p.mergeUniquePatterns();
			    					iter.remove();
			    					break;
			    				}
							}
						}
					}
					/*
					iter = seedMatches.listIterator();
					while ( iter.hasNext() ) { 
						Tuple tuple = iter.next();
						logger1.info("iteration: " + iter);
						logger1.info(tuple.e1 + '\t' + tuple.e2);						
						logger1.info("sentence: " + tuple.sentence);
						for (ReVerbPattern rvb : tuple.ReVerbpatterns) {
							logger1.info("reverb: " + rvb.token_words.toString());
						}
    				}
					// For tuples that don't belong to any existing cluster
					// cluster using DBSCAN eliminate patterns with less than 2 tuples
					System.out.println("\nTuples with low similarity with clusters (discarded)");
					System.out.println(seedMatches.size());
					DBSCAN(seedMatches,patterns);
										
					System.out.println("\nGenerated patterns");
					for (SnowballPattern p: patterns) {
						p.confidence();
						System.out.println("#tuples		:" + p.tuples.size());
						System.out.println(p.patterns);						
						System.out.println("====================================\n");
					}					
				}
				System.out.println();
				*/
				
				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence(Tuple) with each Pattern
				// - Matching Tuple objects are used to score a Pattern confidence, based 
				// 	 on having extracted a relationship which part of the seed set
				
				System.out.println(Config.seedTuples.size() + " tuples in the Seed set");
				System.out.println("Computing similarity of " + Config.e1_type + " - " + Config.e2_type + " tuples with patterns");								
				comparePatternsTuples(candidateTuples, patterns, processedTuples);				
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				// Expand extraction patterns with semantic similar words
				if (Config.expand_patterns==true) {
					Pair<Set<String>, Set<WordEntry>> similar_words = expandPatterns(patterns);
					List<WordEntry> list_similar_words = new LinkedList<WordEntry>(similar_words.getSecond());				
					Collections.sort(list_similar_words);
					LinkedList<WordEntry> top_similar = new LinkedList<WordEntry>(list_similar_words.subList(0, 2));
					
					for (WordEntry wordEntry : top_similar) {						 						
						if (!similar_words.getFirst().contains(wordEntry.name) && wordEntry.score>=0.6) {
							SnowballPattern p = new SnowballPattern();
							System.out.println(wordEntry.name + '\t' + wordEntry.score);
							
							// Create a (virtual) tuple (i.e., no sentence associated) with the word2vec representation
							Tuple t = new Tuple();
							t.middle_words.add(wordEntry.name);
							List<String> words = new LinkedList<String>();
							words.add(wordEntry.name);
							FloatMatrix patternWord2Vec = CreateWord2VecVectors.createVecSum(words);
							t.middleReverbPatternsWord2VecSum.add(patternWord2Vec);
							p.addTuple(t);
							p.expanded=true;
							ReVerbPattern rvb = new ReVerbPattern();
							rvb.token_words = words;
							t.ReVerbpatterns.add(rvb);
							p.mergeUniquePatterns();
							
							// Add it to known patterns, if its different
							if (!patterns.contains(p)) {
								patterns.add(p);
								System.out.println("New pattern generated: ");
								System.out.println(p.patterns);
								System.out.println();
							}
						}						
					}

					System.out.println("\nPatterns and Expanded Patterns");
					for (SnowballPattern pattern : patterns) {
						System.out.println(pattern.patterns);
						System.out.println();
					}					
				}
				
				
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
				
				System.out.println("\nPatterns confidence updated\n");
				for (SnowballPattern p: patterns) {
					p.confidence();
					System.out.println("confidence	:" + p.confidence);
					System.out.println("positive	:" + p.positive);
					System.out.println("negative	:" + p.negative);
					System.out.println("#tuples		:" + p.tuples.size());
					System.out.println(p.patterns);						
					System.out.println("====================================\n");
				}
				
				// Update Tuple confidence based on patterns confidence
				System.out.println("Calculating tuples confidence");
				Snowball.calculateTupleConfidence(candidateTuples);

				/*
				// Print each collected Tuple and its confidence
				ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (Tuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
				*/
									
				// Calculate a new seed set of tuples to use in next iteration, such that:
				// seeds = { T | Conf(T) > min_tuple_confidence }
				System.out.println("Adding tuples with confidence =>" + Config.min_tuple_confidence + " as seed for next iteration");
				int added = 0;
				int removed = 0;				
				for (Tuple t : candidateTuples.keySet()) {
					if (t.confidence>=Config.min_tuple_confidence) {
						Config.seedTuples.add(new Seed(t.e1.trim(),t.e2.trim()));
						added++;
					} else removed++;
				}
				System.out.println(removed + " tuples removed due to confidence lower than " + Config.min_tuple_confidence);				
				System.out.println(added + " tuples added to seed set");
				iter++;
			}			
		}
	}
	
	
	/*
	 * Clusters all the collected Tuples with DBSCAN
	 *  - ReVerb patterns are extracted from the middle context
	 *  - If no patterns are found, the middle words are considered
	 *  - Word2Vec vectors from the ReVerb patterns/middle words are summed
	 *  - Tuples are clustered according to: 1-cos(a,b) where 'a' and' b' are Word2Vec vectors
	 */  
	private static void DBSCAN(LinkedList<Tuple> tuples, List<SnowballPattern> patterns) {
		DistanceMeasure measure = new CosineMeasure();
		double eps = 1-Config.min_degree_match;
		int minPts = 2;
		DBSCANClusterer<Clusterable> dbscan = new DBSCANClusterer<>(eps, minPts, measure);

		// Tuples implement the Clusterable Interface to allow clustering
		// Add all tuples to a Clusterable collection to be passed to DBSCAN
		LinkedList<Clusterable> points = new LinkedList<Clusterable>();
				
		for (Tuple t : tuples) {			
			// Cluster according to ReVerb patterns
			if (t.middleReverbPatternsWord2VecSum.size()>0) points.add(t);
		}
		
		/* Cluster ReVerb patterns/words in middle context */		
		int not_considered = tuples.size()-points.size();
		System.out.println();
		System.out.println("Tuples to cluster	  : " + points.size());
		System.out.println("Tuples not considered : " + not_considered);		
		List<Cluster<Clusterable>> clusters = dbscan.cluster(points);		
		System.out.println("\nClusters generated: " + clusters.size());
		
		/* Transform the clustered tuples into SnowballPattern */
		int c = 1;
		for (Cluster<Clusterable> cluster : clusters) {
			List<Clusterable> objects = cluster.getPoints();
			SnowballPattern pattern = new SnowballPattern();			
			for (Clusterable object : objects) {
				Tuple t = (Tuple) object;
				pattern.tuples.add(t);
			}						
			if (pattern.tuples.size()>=Config.min_pattern_support) {
				patterns.add(pattern);
				pattern.mergeUniquePatterns();
				System.out.println("Cluster " + c );
				System.out.println(pattern.tuples.size() + " tuples");
				System.out.println(pattern.patterns.size() + " unique relational phrases");
				System.out.println(pattern.patterns);
				System.out.println();
				c++;
			}
			else {
				System.out.println("Discarded");
				System.out.println(pattern.patterns.size() + " relational phrases");
				System.out.println(pattern.patterns);
				System.out.println();
			}
			
		}
		System.out.println("\nClusters discarded: " + String.valueOf(clusters.size()-c+1));
	}
		
	static void comparePatternsTuples(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns, List<Tuple> processedTuples) {

		// Compute similarity of a tuple with all the extraction patterns
		// Compare the ReVerb patterns/middle words from the sentence/Tuple 
		// with every extraction pattern from the clusters/SnowballPattern
		
		int count = 0;
		System.out.println("Tuples to analyze   : " + processedTuples.size());
		System.out.println("Extraction patterns : " + patterns.size());
		for (Tuple tuple : processedTuples) {
			if (count % 10000==0) System.out.print(".");
			List<Integer> patternsMatched = new LinkedList<Integer>();
			double simBest = 0;
			double similarity = 0;
			SnowballPattern patternBest = null;			
			simBest = Double.NEGATIVE_INFINITY;
			if (tuple.ReVerbpatterns.size()==0) continue;
			for (SnowballPattern pattern : patterns) {
				// Compare the tuple with the sum in Pattern
				if (tuple.ReVerbpatterns.size()>0) {
					FloatMatrix b = CreateWord2VecVectors.createVecSum(tuple.ReVerbpatterns.get(0).token_words);
					similarity = TermsVector.cosSimilarity(pattern.w2v_centroid, b);    				
					if (similarity>=Config.min_degree_match) {
	    				patternsMatched.add(patterns.indexOf(pattern));
	    				pattern.updatePatternSelectivity(tuple.e1,tuple.e2);
	    				if (iter>0) {
	    					pattern.confidence_old = pattern.confidence;	        						
	    					pattern.RlogF_old = pattern.RlogF;
	    				}
	    				pattern.confidence();        						
	    				if (Config.use_RlogF=true) {	        						
	    					pattern.ConfidencePatternRlogF();
	    				}
	    				if (similarity>=simBest) {
	    					simBest = similarity;
	    					patternBest = pattern;    
	    				}
	    			}
				}
				
				if (simBest>=Config.min_degree_match) {
					List<Pair<SnowballPattern, Double>> list = null;
					Pair<SnowballPattern,Double> p = new Pair<SnowballPattern, Double>(patternBest, simBest);

					// Check if the tuple was already extracted in a previous iteration
					Tuple tupleInCandidatesMap = null;	        					
					for (Tuple extractedT : candidateTuples.keySet()) {
						if (tuple.equals(extractedT)) {
							tupleInCandidatesMap = extractedT;        							
						}        						
					}
					
					// If the tuple was not seen before:
					//  - associate it with this Pattern and similarity score 
					//  - add it to the list of candidate Tuples
					if ( tupleInCandidatesMap == null ) {
						list = new LinkedList<Pair<SnowballPattern,Double>>();        						
						list.add(p);
						tupleInCandidatesMap = tuple;
					}
					// If the tuple was already extracted:
					//  - associate this Pattern and similarity score with the Tuple    
					else {        						
						list = candidateTuples.get(tupleInCandidatesMap);
						if (!list.contains(p)) list.add(p);       						
					}
					candidateTuples.put(tupleInCandidatesMap, list);
				}

				// Use confidence values from past iterations to calculate pattern confidence: 
				// updateConfidencePattern()
				if (iter>0) {							
					for (Integer i : patternsMatched) {
						SnowballPattern p = patterns.get(i);
						p.updateConfidencePattern();
						p.confidence_old = p.confidence;
						p.RlogF_old = p.RlogF;
					}							
				}
			}
			
			count++;
    				
			// If the similarity with the majority is > threshold, add it
			/*
			int good = 0;
			int bad = 0;
			for (List<String> patternTokens : p.patterns) {
				FloatMatrix a = CreateWord2VecVectors.createVecSum(patternTokens);
				FloatMatrix b = CreateWord2VecVectors.createVecSum(tuple.ReVerbpatterns.get(0).token_words);
				double score = TermsVector.cosSimilarity(a, b);
				
				System.out.println("pattern: " + patternTokens);
				System.out.println("tuple: " + tuple.ReVerbpatterns.get(0).token_words);
				System.out.println("score: " + score);
				System.out.println();
										
				if (score>=Config.min_degree_match) {
					good++;
					if (score>bestScore) {
						bestScore = score;
					}
				}
				else bad++;
			}
			if (good>bad) {
				matchedPattern=true;
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
			*/

		/*
		if (matchedPattern==false) {
			logger2.info("iteration: " + iter);
			logger2.info(tuple.e1 + '\t' + tuple.e2);						
			logger2.info("sentence: " + tuple.sentence);
			for (ReVerbPattern rvb : tuple.ReVerbpatterns) {
				logger2.info("reverb: " + rvb.token_words.toString());
			}    				
		}
		*/
		}
	}
		
	/*
	 * Expands the relationship words of a pattern based on similarities
	 * 	- For each word part of a pattern
	 * 		- Construct a set with all the similar words according to Word2Vec given a threshold t    	 
	 *  	- Calculate the intersection of all sets
	 */
	private static Pair<Set<String>, Set<WordEntry>> expandPatterns(List<SnowballPattern> patterns) {
		Set<String> words = new HashSet<String>();
		System.out.println("Using " + patterns.size() + " pattern to expand relational words");
		// Consider only patterns with high confidence
		for (SnowballPattern p : patterns) {
			if (p.confidence<0.6) continue;
			for (Tuple tuple : p.tuples) {
				ReVerbPattern reverb = tuple.ReVerbpatterns.get(0);
				// If its not an expanded pattern it has PoS-tags 
				// Select main verbs if it has a ReVerb pattern
				// and nouns if it does not contain a ReVerb pattern
				if (p.expanded!=true) {					
					// If its a ReVerb pattern it has a verb, extract the verb
					if (tuple.hasReVerbPatterns) {
						for (int i = 0; i < reverb.token_ptb_pos_tags.size(); i++) {
							if (reverb.token_ptb_pos_tags.get(i).equalsIgnoreCase("VBN")) {
								words.add(reverb.token_words.get(i));
							}
						}						
					}
					// If its not a ReVerb pattern extract nouns
					else {
						for (int i = 0; i < reverb.token_ptb_pos_tags.size(); i++) {
							if (reverb.token_ptb_pos_tags.get(i).equalsIgnoreCase("NN")) {
								words.add(reverb.token_words.get(i));
							}
						}						
					}
				}
				// If its pattern that was generate from other extraction pattern there 
				// are no PoS tags associated just words
				else {
					words.addAll(reverb.token_words);
				}
				// Some of the words can be added due to errors in PoS-tagging, this filter remove stop words
				for (Iterator<String> iterator = words.iterator(); iterator.hasNext();) {
					String w = (String) iterator.next();
					if (Stopwords.stopwords.contains(w)) {
						iterator.remove();
					}
				} 
			}
		}
		//TODO: normalize with Morphadoner ?
		// Method 1: For each vector word get the top closest words, use only common words to all
		List<Set<WordEntry>> similar_words = new LinkedList<Set<WordEntry>>();

		for (String word : words) {
			Set<WordEntry> similar = Config.word2vec.distance(word);
			Set<WordEntry> top = new HashSet<WordEntry>();
			for (WordEntry wordEntry : similar) {
				if (wordEntry.score>0.6) top.add(wordEntry);
			}
			similar_words.add(top);
		}
		/*
		for (Set<WordEntry> set : similar_words) {
			set.r
		}
		*/
		
		
		// Method 2: Generate a vector by summing each relational word, get the closest word to that vector  
		System.out.println(words);
		System.out.println();
		Set<WordEntry> similar = Config.word2vec.distance(new LinkedList<String>(words));
		/*
		for (WordEntry wordEntry : similar_words) {
			System.out.println(wordEntry.name + '\t' + wordEntry.score);				
		}
		*/
		Pair<Set<String>, Set<WordEntry>> p = new Pair<Set<String>, Set<WordEntry>>(words, similar);
		return p;
	}
}







