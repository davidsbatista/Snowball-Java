package bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.ReVerbPattern;
import nlp.Stopwords;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jblas.FloatMatrix;

import tuples.REDSTuple;
import tuples.Seed;
import tuples.Tuple;
import utils.Pair;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import word2vec.com.ansj.vec.domain.WordEntry;
import clustering.REDSPattern;
import clustering.Singlepass;
import clustering.SnowballPattern;
import clustering.dbscan.CosineMeasure;

public class REDS {

	//private static final Logger logger1 = Logger.getLogger( REDS.class.getName() );	
	//private static final Logger logger2 = Logger.getLogger( REDS.class.getName() );
	public static int iter = 0;
		
	public static void start(String sentencesFile, String seedsFile,Map<REDSTuple, List<Pair<REDSPattern, Double>>> candidateTuples, List<REDSPattern> patterns) throws IOException, Exception {		
		
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
		SnowballConfig.readSeeds(seedsFile);
		long startTime = System.nanoTime();		
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	static void iteration(long startTime, String sentencesFile, Map<REDSTuple, List<Pair<REDSPattern, Double>>> candidateTuples, List<REDSPattern> patterns) throws IOException, Exception {					
		
		List<REDSTuple> processedTuples = new LinkedList<REDSTuple>();
		File f = new File("REDS_processed_tuples.obj");
		
		if (!f.exists()) {
			System.out.println("\nPre-processing data");
			generateTuples(sentencesFile,processedTuples);
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
			processedTuples = (List<REDSTuple>) objectInput.readObject();
			System.out.println("\n"+processedTuples.size() + " tuples gathered");						
			in.close();
		}
		
		while (iter<=SnowballConfig.number_iterations) {
			// Collect sentences (Tuple objects) where both entities occur 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Seed matches:\n");			
			LinkedList<REDSTuple> seedMatches = matchSeedsTuples(processedTuples);
			
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
				Singlepass.SinglePassREDS(seedMatches,patterns);
				System.out.println("\n"+patterns.size() + " patterns generated");					
				if (patterns.size()==0) {
					System.out.println("No patterns generated");
					System.exit(0);
				}
				
				/* TODO: Eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<REDSPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					REDSPattern p = patternIter.next();
					if (p.tuples.size()<SnowballConfig.min_pattern_support) patternIter.remove();
				}
				patternIter = null;
				System.out.println(patterns.size() + " patterns supported by at least " + SnowballConfig.min_pattern_support + " tuple(s)");
				*/
								
				/* Maneira para comparar/avaliar patterns aprendidos */
				// TODO: Dump initial patterns	
				// Compare with all learned patterns
				// Calculate how many different relational/words pattern were learned
				// How many extracted valid patterns

				
				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence(Tuple) with each Pattern
				// - Matching Tuple objects are used to score a Pattern confidence, based 
				// 	 on having extracted a relationship which part of the seed set
				
				System.out.println(REDSConfig.seedTuples.size() + " tuples in the Seed set");
				System.out.println("Computing similarity of " + REDSConfig.e1_type + " - " + REDSConfig.e2_type + " tuples with patterns");								
				comparePatternsTuples(candidateTuples, patterns, processedTuples);				
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				// Expand extraction patterns with semantic similar words
				if (REDSConfig.expand_patterns==true) {
					
					Set<String> similar_words = expandPatterns(patterns);
					
					for (String string : similar_words) {
						System.out.println(string);
					}
					
					/*
					List<String> list_similar_words = new LinkedList<String>(similar_words);					
					Collections.sort(list_similar_words);
					
					LinkedList<String> top_similar = new LinkedList<String>(list_similar_words.subList(0, 2));
					
					for (String wordEntry : top_similar) {						 						
						if (!similar_words.getFirst().contains(wordEntry.name) && wordEntry.score>=0.6) {
							REDSPattern p = new REDSPattern();
							System.out.println(wordEntry.name + '\t' + wordEntry.score);
							
							// Create a (virtual) tuple (i.e., no sentence associated with) with the word2vec representation
							REDSTuple t = new REDSTuple();
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
					*/

					System.out.println("\nPatterns and Expanded Patterns");
					for (REDSPattern pattern : patterns) {
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
				for (REDSPattern p: patterns) {
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
				calculateTupleConfidence(candidateTuples);

				/*
				// Print each collected Tuple and its confidence
				ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (Tuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
				*/
									
				// Calculate a new seed set of tuples to use in next iteration, such that:
				// seeds = { T | Conf(T) > min_tuple_confidence }
				System.out.println("Adding tuples with confidence =>" + SnowballConfig.min_tuple_confidence + " as seed for next iteration");
				int added = 0;
				int removed = 0;				
				for (REDSTuple t : candidateTuples.keySet()) {
					if (t.confidence>=SnowballConfig.min_tuple_confidence) {
						SnowballConfig.seedTuples.add(new Seed(t.e1.trim(),t.e2.trim()));
						added++;
					} else removed++;
				}
				System.out.println(removed + " tuples removed due to confidence lower than " + SnowballConfig.min_tuple_confidence);				
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
		double eps = 1-SnowballConfig.min_degree_match;
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
			if (pattern.tuples.size()>=SnowballConfig.min_pattern_support) {
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
		
	static void comparePatternsTuples(Map<REDSTuple, List<Pair<REDSPattern, Double>>> candidateTuples, List<REDSPattern> patterns, List<REDSTuple> processedTuples) {

		// Compute similarity of a tuple with all the extraction patterns
		// Compare the relational words from the sentence with every extraction pattern
		
		int count = 0;
		System.out.println("Tuples to analyze   : " + processedTuples.size());
		System.out.println("Extraction patterns : " + patterns.size());
		for (REDSTuple tuple : processedTuples) {
			
			if (count % 10000==0) System.out.print(".");
			List<Integer> patternsMatched = new LinkedList<Integer>();
			double simBest = 0;
			double similarity = 0;
			REDSPattern patternBest = null;			
			simBest = Double.NEGATIVE_INFINITY;
			if (tuple.ReVerbpatterns.size()==0) continue;
			
			for (REDSPattern extractionPattern : patterns) {				
				if (tuple.ReVerbpatterns.size()>0) {					
					List<String> relationalWords = tuple.ReVerbpatterns.get(0).token_words;
					
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
					}
					
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
							similarity = result.getSecond();
						}
						else {
							similarity = result.getSecond();
						}
					}
					
					
					if (similarity>=REDSConfig.similarity_threshold) {
	    				patternsMatched.add(patterns.indexOf(extractionPattern));
	    				extractionPattern.updatePatternSelectivity(tuple.e1,tuple.e2);
	    				if (iter>0) {
	    					extractionPattern.confidence_old = extractionPattern.confidence;	        						
	    					extractionPattern.RlogF_old = extractionPattern.RlogF;
	    				}
	    				extractionPattern.confidence();
	    				if (similarity>=simBest) {
	    					simBest = similarity;
	    					patternBest = extractionPattern;    
	    				}
	    			}
				}
				
				// Associate the relationship instance with the pattern with the maximum similarity score
				
				if (simBest>=REDSConfig.similarity_threshold) {
					List<Pair<REDSPattern, Double>> list = null;
					Pair<REDSPattern,Double> p = new Pair<REDSPattern, Double>(patternBest, simBest);

					// Check if the tuple was already extracted in a previous iteration
					REDSTuple tupleInCandidatesMap = null;	        					
					for (REDSTuple extractedT : candidateTuples.keySet()) {
						if (tuple.equals(extractedT)) {
							tupleInCandidatesMap = extractedT;        							
						}        						
					}
					
					// If the tuple was not seen before:
					//  - associate it with this Pattern and similarity score 
					//  - add it to the list of candidate Tuples
					if ( tupleInCandidatesMap == null ) {
						list = new LinkedList<Pair<REDSPattern,Double>>();        						
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

				// Update confidence using values from past iterations to calculate pattern confidence: 
				// updateConfidencePattern()
				if (iter>0) {							
					for (Integer i : patternsMatched) {
						REDSPattern p = patterns.get(i);
						p.updateConfidencePattern();
						p.confidence_old = p.confidence;
						//p.RlogF_old = p.RlogF;
					}							
				}
			}			
			count++;			
		}
	}

	/*
	 * Expands the relationship words of a pattern based on similarities
	 * 	- For each word part of a pattern
	 * 		- Construct a set with all the similar words according to Word2Vec given a threshold t    	 
	 *  	- Calculate the intersection of all sets
	 */
	private static Set<String> expandPatterns(List<REDSPattern> patterns) {
		
		//TODO: fazer o mesmo, mas seleccionar apenas verbos e nouns
		/*
		Set<String> words = new HashSet<String>();		
		System.out.println("Using " + patterns.size() + " pattern to expand relational words");
		
		for (REDSPattern p : patterns) {			
			//TODO: expandir apenas patterns com a confiança alta
			if (p.confidence<0.6) continue;
			
			for (REDSTuple tuple : p.tuples) {
				
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
						
			ReVerbPattern reverb = tuple.ReVerbpatterns.get(0);
			
			List<Set<WordEntry>> similar_words = new LinkedList<Set<WordEntry>>();

			for (String word : words) {
				Set<WordEntry> similar = SnowballConfig.word2vec.distance(word);
				Set<WordEntry> top = new HashSet<WordEntry>();
				for (WordEntry wordEntry : similar) {
					if (wordEntry.score>0.6) top.add(wordEntry);
				}
				similar_words.add(top);
			}

			for (Set<WordEntry> set : similar_words) {
				set.r
			}
		}
		*/
		
		Set<String> words = new HashSet<String>();
		
		// For each vector word get the top closest words, use only common words to all
		// TODO: normalize verbs (with Morphadoner) ?
		// TODO: expandir apenas patterns com a confiança alta
		
		if (REDSConfig.expansion=="common-words") {
			
			for (REDSPattern p : patterns) {
				for (REDSTuple tuple : p.tuples) {					
					List<String> relationalWords = tuple.ReVerbpatterns.get(0).token_words;
					FloatMatrix vectorPattern = null;					
					if (REDSConfig.single_vector.equalsIgnoreCase("sum")) {
						vectorPattern = CreateWord2VecVectors.createVecSum(relationalWords);
					}					
					else if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
						vectorPattern = CreateWord2VecVectors.createVecCentroid(relationalWords);
					}
					
					Set<WordEntry> similar_words = REDSConfig.word2vec.distance(vectorPattern.data, REDSConfig.top_k);
					
					for (WordEntry wordEntry : similar_words) {
						words.add(wordEntry.name);
					}
				}
			}
		}
		
		// Generate a single vector from all the patterns, find the top-k words closest that vector
		else if (REDSConfig.expansion=="single-vector") {
			
			FloatMatrix vector = null;
			FloatMatrix vectorPattern = null;
			
			for (REDSPattern p : patterns) {
				for (REDSTuple tuple : p.tuples) {										
					List<String> relationalWords = tuple.ReVerbpatterns.get(0).token_words;					
					if (REDSConfig.single_vector.equalsIgnoreCase("sum")) {
						vectorPattern = CreateWord2VecVectors.createVecSum(relationalWords);
					}					
					else if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
						vectorPattern = CreateWord2VecVectors.createVecCentroid(relationalWords);
					}				
				}				
				vector.addi(vectorPattern);
			}
			
			if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
				vector = vector.div((float) patterns.size()); 
			}									

			Set<WordEntry> similar_words = SnowballConfig.word2vec.distance(vector.data, REDSConfig.top_k);
			
			for (WordEntry wordEntry : similar_words) {				
				words.add(wordEntry.name);
			}
		}
		
		return words;
	}
	
	
	
	
	
	static LinkedList<REDSTuple> matchSeedsTuples(List<REDSTuple> processedTuples) {
		
		Map<Seed,Integer> counts = new HashMap<Seed, Integer>();
		LinkedList<REDSTuple> matchedTuples = new LinkedList<>();
		int processed = 0;
		for (REDSTuple tuple : processedTuples) {
			if (processed % 10000==0) System.out.println(processed + " of " + processedTuples.size());
			for (Seed seed : SnowballConfig.seedTuples) {
				if (tuple.e1.equalsIgnoreCase(seed.e1) && tuple.e2.equalsIgnoreCase(seed.e2)) {
					matchedTuples.add(tuple);					
					Integer count = counts.get(seed);
					if (count==null) counts.put(seed, 1);
					else counts.put(seed, ++count);
				}
			}
			processed++;
		}
		
		/* Print number of seed matches sorted by descending order */
		/*
		System.out.println();
		ArrayList<Map.Entry<Seed,Integer>> myArrayList = new ArrayList<Map.Entry<Seed, Integer>>(counts.entrySet());		
		Collections.sort(myArrayList, new SortMaps.StringIntegerComparator());		
		Iterator<Entry<Seed, Integer>> itr=myArrayList.iterator();
		Seed key=null;
		int value=0;
		while(itr.hasNext()){
			Map.Entry<Seed,Integer> e = itr.next();		
			key = e.getKey();
			value = e.getValue().intValue();		
			System.out.println(key.e1 + '\t'+ key.e2 +"\t" + value);
		}
		for (Seed s : Config.seedTuples) if (counts.get(s) == null) System.out.println(s.e1 + '\t' + s.e2 + "\t 0 tuples");
		*/
		return matchedTuples;
	}
	
	static void generateTuples(String file, List<REDSTuple> processedTuples) throws Exception {
		String sentence = null;
		String e1_begin = "<"+SnowballConfig.e1_type+">";
		String e1_end = "</"+SnowballConfig.e1_type+">";
		String e2_begin = "<"+SnowballConfig.e2_type+">";
		String e2_end = "</"+SnowballConfig.e2_type+">";		
		Pattern pattern1 = Pattern.compile(e1_begin+"[^<]+"+e1_end);
		Pattern pattern2 = Pattern.compile(e2_begin+"[^<]+"+e2_end);		
		BufferedReader f1 = new BufferedReader(new FileReader(new File(file)));
		int count = 0;
		// Find all possible pairs of seed e1_type and e2_type
		while ( ( sentence = f1.readLine() ) != null) {
			if (count % 10000 == 0) System.out.print(".");
			sentence = sentence.trim();			
			Matcher matcher1 = pattern1.matcher(sentence);
			Matcher matcher2 = pattern2.matcher(sentence);			
			// Make sure e2 is not the same as e1, if e1 and e2 have the same type
			// Just run matcher2.find() to match the next occurrence
			boolean found1 = matcher1.find();
			boolean found2 = matcher2.find();
			if (SnowballConfig.e1_type.equals(SnowballConfig.e2_type) && found1) {
				found2 = matcher2.find();
			}		
			
			try {
				String e1 = (sentence.substring(matcher1.start(),matcher1.end())).replaceAll("<[^>]+>"," ");
				String e2 = (sentence.substring(matcher2.start(),matcher2.end())).replaceAll("<[^>]+>"," ");
				if ( (!SnowballConfig.e1_type.equals(SnowballConfig.e2_type) && matcher2.end()<matcher1.end() || matcher2.start()<matcher1.end())) continue;								
				if ( (found1 && found2) && matcher1.end()<matcher2.end()) {
					
					// Ignore contexts where another entity occur between the two entities
					String middleText = sentence.substring(matcher1.end(),matcher2.start());
            		Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
            		Matcher matcher = ptr.matcher(middleText);            		
	            	if (matcher.find()) continue;
	            	
					// Consider only tokens, name-entities are not part of the considered vocabulary               		
	            	String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String right_txt = sentence.substring(matcher2.end()+1).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	        		String[] middle_tokens = middle_txt.split("\\s");
	        		
	                if (middle_tokens.length<=SnowballConfig.max_tokens_away && middle_tokens.length>=SnowballConfig.min_tokens_away) {	                	
	                	// Create a Tuple for an occurrence found        				
	        			REDSTuple t = new REDSTuple(left_txt, middle_txt, right_txt, e1.trim(), e2.trim(), sentence);	        			
	        			processedTuples.add(t);        			
	                }
				}
				
			} catch (java.lang.IllegalStateException e) {
				/*
				System.out.println(e.getLocalizedMessage());
				System.out.println(e.getStackTrace());
				*/				
				}			
		count++;
		}
		f1.close();
	}
	
	/*
	 * Calculates the confidence of a tuple is: Conf(P_i) * DegreeMatch(P_i)
	 */	
	static void calculateTupleConfidence(Map<REDSTuple, List<Pair<REDSPattern, Double>>> candidateTuples) throws IOException {
		for (REDSTuple t : candidateTuples.keySet()) {			
			double confidence = 1;
			if (iter>0) {
				t.confidence_old = t.confidence;
			}
			List<Pair<REDSPattern, Double>> listPatterns = candidateTuples.get(t);
			for (Pair<REDSPattern, Double> pair : listPatterns) {
				confidence *= ( 1 - (pair.getFirst().confidence() * pair.getSecond()) );
			}
			t.confidence = 1 - confidence;
			// If tuple was already seen use past confidence values to calculate new confidence 
			if (iter>0) {
				t.confidence = t.confidence * SnowballConfig.wUpdt + t.confidence_old * (1 - SnowballConfig.wUpdt);
			}
		}
	}
}







