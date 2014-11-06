package bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.ReVerbPattern;
import nlp.Stopwords;

import org.jblas.FloatMatrix;

import tuples.BREDSTuple;
import tuples.Seed;
import utils.Pair;
import utils.SortMaps;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import word2vec.com.ansj.vec.domain.WordEntry;
import clustering.BREDSPattern;
import clustering.Singlepass;

public class BREDS {

	//private static final Logger logger1 = Logger.getLogger( REDS.class.getName() );	
	//private static final Logger logger2 = Logger.getLogger( REDS.class.getName() );
	public static int iter = 0;
		
	public static void start(String sentencesFile, String seedsFile,Map<BREDSTuple, List<Pair<BREDSPattern, Double>>> candidateTuples, List<BREDSPattern> patterns) throws IOException, Exception {		
		
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
		BREDSConfig.readSeeds(seedsFile);
		long startTime = System.nanoTime();		
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	
	static void iteration(long startTime, String sentencesFile, Map<BREDSTuple, List<Pair<BREDSPattern, Double>>> candidateTuples, List<BREDSPattern> patterns) throws IOException, Exception {					
		
		List<BREDSTuple> processedTuples = new LinkedList<BREDSTuple>();
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
			processedTuples = (List<BREDSTuple>) objectInput.readObject();
			System.out.println("\n"+processedTuples.size() + " tuples gathered");						
			in.close();
		}
		
		List<BREDSPattern> goodPatterns = null;
		
		while (iter<=BREDSConfig.number_iterations) {
			// Collect sentences (Tuple objects) where both entities occur 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Seed matches:\n");			
			LinkedList<BREDSTuple> seedMatches = matchSeedsTuples(processedTuples);
			
			
			System.out.println("Seed matches: " + seedMatches.size());
			for (BREDSTuple tuple : seedMatches) {
				System.out.println(tuple.sentence);
				for (ReVerbPattern rvb : tuple.ReVerbpatterns) {
					System.out.println(rvb.token_words);
					System.out.println(rvb.token_universal_pos_tags);
				}
				System.out.println();
			}
			System.out.println();			
								
			if (seedMatches.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			else {
				Singlepass.SinglePassBREDS(seedMatches,patterns);
				System.out.println("\n"+patterns.size() + " patterns generated");					
				if (patterns.size()==0) {
					System.out.println("No patterns generated");
					System.exit(0);
				}
				
				// Eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<BREDSPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					BREDSPattern p = patternIter.next();
					if (p.tuples.size()<BREDSConfig.min_pattern_support) patternIter.remove();
				}
				patternIter = null;
				System.out.println(patterns.size() + " patterns supported by at least " + BREDSConfig.min_pattern_support + " tuple(s)");

				/*
				for (int j = 0; j < patterns.size(); j++) {
					System.out.println("Cluster " + j );
					for (BREDSTuple t : patterns.get(j).tuples) {
						System.out.println(t.ReVerbpatterns.get(0).token_words);
					}				
					System.out.println();
				}
				*/
								
				if (BREDSConfig.pattern_drift==true) {					
					System.out.println("\nDetecting and eliminating semantic drifting patterns..");
					System.out.println();					
					if (iter==0) {
						// assume the first generated patterns are all good
						goodPatterns = new LinkedList<BREDSPattern>(patterns);					
					}
					else {
						// in further iterations keep only patterns that are semantic similar to the goodPatterns 
						for (Iterator<BREDSPattern> iter = patterns.iterator(); iter.hasNext(); ) {
							BREDSPattern pattern = iter.next();
							if (!goodPatterns.contains(pattern)) {							
								if (patternDrifts(goodPatterns, pattern)) iter.remove();
								else goodPatterns.add(pattern);
							}							
						}						
					}
				}
				else goodPatterns = new LinkedList<BREDSPattern>(patterns);
				
				/*
				 * - Look for sentences with occurrence of seeds semantic types (e.g., ORG - LOC)
				 * - Measure the similarity of each sentence(Tuple) with each Pattern
				 * - Matching Tuple objects are used to score a Pattern confidence, based 
				 *	 on having extracted a relationship which part of the seed set
				 */ 								
				System.out.println("Computing similarity of " + BREDSConfig.e1_type + " - " + BREDSConfig.e2_type + " tuples with patterns");								
				comparePatternsTuples(candidateTuples, goodPatterns, processedTuples);				
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				
				// Expand extraction patterns with semantic similar words
				if (BREDSConfig.expand_patterns==true) {					
					System.out.println("\nExpanding extraction patterns");
					Set<String> similar_words = expandPatterns(goodPatterns);
					System.out.println("similar words:" + similar_words);					
					for (String word : similar_words) {
						BREDSPattern p = new BREDSPattern();
						p.expanded=true;						
						// Create a (virtual) tuple (i.e., no sentence associated with) with the word2vec representation						
						BREDSTuple t = new BREDSTuple();
						ReVerbPattern rvb = new ReVerbPattern();
						rvb.token_words.add(word);
						t.ReVerbpatterns.add(rvb);						
						p.addTuple(t);						
						p.mergeUniquePatterns();						
						System.out.println("Expanded pattern: ");
						System.out.println(p.patterns);						
						goodPatterns.add(p);
					}						
					System.out.println("\nPatterns and Expanded Patterns");
					for (BREDSPattern pattern : goodPatterns) {
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
				for (BREDSPattern p: goodPatterns) {
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

				// Print each collected Tuple and its confidence
				ArrayList<BREDSTuple> tuplesOrdered  = new ArrayList<BREDSTuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (BREDSTuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
									
				// Calculate a new seed set of tuples to use in next iteration, such that:
				// seeds = { T | Conf(T) > min_tuple_confidence }
				System.out.println("Adding tuples with confidence =>" + BREDSConfig.instance_confidance + " as seed for next iteration");
				int added = 0;
				int removed = 0;				
				for (BREDSTuple t : candidateTuples.keySet()) {
					if (t.confidence>=BREDSConfig.instance_confidance) {
						BREDSConfig.seedTuples.add(new Seed(t.e1.trim(),t.e2.trim()));
						added++;
					} else removed++;
				}
				System.out.println(removed + " tuples removed due to confidence lower than " + BREDSConfig.instance_confidance);				
				System.out.println(added + " tuples added to seed set");
				iter++;
			}			
		}
	}
	
	
	static boolean patternDrifts(List<BREDSPattern> goodPatterns, BREDSPattern newPattern) {		
		boolean patternDrifts = true;
		newPattern.mergeUniquePatterns();		
		for (BREDSPattern goodPattern : goodPatterns) {
			goodPattern.mergeUniquePatterns();			
			if (semanticSimilar(goodPattern.patterns,newPattern.patterns)) patternDrifts = false;			
		}
		return patternDrifts;		
	}


	static boolean semanticSimilar(Set<List<String>> patterns1, Set<List<String>> patterns2) {		
		int positive = 0;
		int negative = 0;		
		for (List<String> pattern_tokens1 : patterns1) {			
			FloatMatrix vectorPattern1 = null;			
			if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) vectorPattern1 = CreateWord2VecVectors.createVecSum(pattern_tokens1);					
			else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid"))vectorPattern1 = CreateWord2VecVectors.createVecCentroid(pattern_tokens1);			
			for (List<String> pattern_tokens2 : patterns2) {
				FloatMatrix vectorPattern2 = null;			
				if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) vectorPattern2 = CreateWord2VecVectors.createVecSum(pattern_tokens2);					
				else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid"))vectorPattern2 = CreateWord2VecVectors.createVecCentroid(pattern_tokens2);				
				double sim = TermsVector.cosSimilarity(vectorPattern1, vectorPattern2);
				if (sim>=BREDSConfig.threshold_similarity) {
					positive++;
				}
				else negative++;
			}			
		}
		
		if (positive>negative) {
			return true;
		}
		else {
			return false;
		}
	}
		
	static void comparePatternsTuples(Map<BREDSTuple, List<Pair<BREDSPattern, Double>>> candidateTuples, List<BREDSPattern> patterns, List<BREDSTuple> processedTuples) {

		// Compute similarity of a tuple with all the extraction patterns
		// Compare the relational words from the sentence with every extraction pattern
		
		int count = 0;
		System.out.println("Tuples to analyze   : " + processedTuples.size());
		System.out.println("Extraction patterns : " + patterns.size());
		
		for (BREDSPattern p : patterns) {
			System.out.println(p.patterns);
		}		
		
		for (BREDSTuple tuple : processedTuples) {			
			if (count % 10000==0) System.out.print(".");
			List<Integer> patternsMatched = new LinkedList<Integer>();
			double simBest = 0;
			double similarity = 0;
			BREDSPattern patternBest = null;			
			simBest = Double.NEGATIVE_INFINITY;
			if (tuple.ReVerbpatterns.size()==0) continue;
			
			for (BREDSPattern extractionPattern : patterns) {		
				if (tuple.ReVerbpatterns.size()>0) {
					
					FloatMatrix patternVector = null;
					FloatMatrix sentence = tuple.relationalWordsVector.get(0);
					
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
					}
					
					/*
					System.out.println("sentence	: " + tuple.sentence);
					System.out.println("rel			: " + relationalWords);
					System.out.println("pattern		: " + extractionPattern.patterns);
					System.out.println("similarity	: " + similarity);
					System.out.println();
					*/
										
					
					// update patterns confidence based on matches					
					if (similarity>=BREDSConfig.threshold_similarity) {
						
						/*
						System.out.println("sentence:  " + tuple.sentence);
						System.out.println("relational words		 : " + relationalWords);
						System.out.println("pattern relational words : " + extractionPattern.patterns);						
						System.out.println("cosine 					 : " + similarity);
						System.out.println();
						*/
											
	    				patternsMatched.add(patterns.indexOf(extractionPattern));
	    				extractionPattern.updatePatternSelectivity(tuple.e1,tuple.e2);
	    				if (iter>0) {
	    					extractionPattern.confidence_old = extractionPattern.confidence;	        						
	    					extractionPattern.RlogF_old = extractionPattern.RlogF;
	    				}
	    				extractionPattern.confidence();
	    				if (similarity >= simBest) {
	    					simBest = similarity;
	    					patternBest = extractionPattern;    
	    				}
	    				
	    				/*
	    				System.out.println(tuple.e1 + '\t' + tuple.e2);
	    				System.out.println(extractionPattern.patterns);
	    				System.out.println("confidence:" + extractionPattern.confidence);
	    				System.out.println();
	    				*/
	    			}
				}
				
				// Associate the relationship instance with the pattern the has maximum similarity score				
				if ( simBest >= BREDSConfig.threshold_similarity ) {
					
					List<Pair<BREDSPattern, Double>> list = null;
					Pair<BREDSPattern,Double> p = new Pair<BREDSPattern, Double>(patternBest, simBest);
					
					/*
					System.out.println(tuple.sentence);
					System.out.println(tuple.ReVerbpatterns.get(0).token_words);
					System.out.println(patternBest.patterns);
					System.out.println("similarity	:	" + simBest);
					System.out.println("pattern		:	" + patternBest.confidence);
					System.out.println();
					*/					

					// Check if the tuple was already extracted in a previous iteration
					BREDSTuple tupleInCandidatesMap = null;	        					
					for (BREDSTuple extractedT : candidateTuples.keySet()) {
						if (tuple.equals(extractedT)) {
							tupleInCandidatesMap = extractedT;        							
						}        						
					}
					
					// If the tuple was not seen before:
					//  - associate it with this Pattern and similarity score 
					//  - add it to the list of candidate Tuples
					if ( tupleInCandidatesMap == null ) {
						list = new LinkedList<Pair<BREDSPattern,Double>>();        						
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
						BREDSPattern p = patterns.get(i);
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
	private static Set<String> expandPatterns(List<BREDSPattern> patterns) {
		
		Set<String> words = new HashSet<String>();		
		// For each vector word get the top closest words, use only common words to all
				
		if (BREDSConfig.expansion.equalsIgnoreCase("common-words")) {
			for (BREDSPattern p : patterns) {
				
				if (p.expanded!=true) {
					
					System.out.println(p.patterns+"\n");
					
					Set<String> relationalWords = new HashSet<String>();
					FloatMatrix vectorPattern = null;
				
					for (BREDSTuple tuple : p.tuples) {					
						ReVerbPattern reverb = tuple.ReVerbpatterns.get(0);						
						// If its a ReVerb pattern it has a verb, extract the verb 
						if (tuple.hasReVerbPatterns) {
							for (int i = 0; i < reverb.token_ptb_pos_tags.size(); i++) {
								if (reverb.token_ptb_pos_tags.get(i).equalsIgnoreCase("VBN")) {
									relationalWords.add(reverb.token_words.get(i));
								}
							}						
						}
						// If its not a ReVerb pattern extract nouns
						else {
							for (int i = 0; i < reverb.token_ptb_pos_tags.size(); i++) {
								if (reverb.token_ptb_pos_tags.get(i).equalsIgnoreCase("NN")) {
									relationalWords.add(reverb.token_words.get(i));
								}
							}						
						}
					}
					
					System.out.println("pattern word: " + relationalWords);
					
					if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) vectorPattern = CreateWord2VecVectors.createVecSum(relationalWords);					
					else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) vectorPattern = CreateWord2VecVectors.createVecCentroid(relationalWords);
					
					Set<WordEntry> similar_words = BREDSConfig.word2vec.distance(vectorPattern.data, BREDSConfig.top_k);
						
					System.out.println("similar words:");
					
					for (WordEntry wordEntry : similar_words) {
						System.out.println(wordEntry.name);
						if (!Stopwords.stopwords.contains(wordEntry.name)) {
							words.add(wordEntry.name);
						}
					}
					System.out.println();
				}
			}
		}
				
		// Generate a single vector from all the patterns, find the top-k words closest that vector
		else if (BREDSConfig.expansion.equalsIgnoreCase("single-vector")) {
			
			FloatMatrix vector =  new FloatMatrix(BREDSConfig.word2Vec_dim);
			FloatMatrix vectorPattern = null;
			
			for (BREDSPattern p : patterns) {
				for (BREDSTuple tuple : p.tuples) {										
					List<String> relationalWords = tuple.ReVerbpatterns.get(0).token_words;					
					if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) {
						vectorPattern = CreateWord2VecVectors.createVecSum(relationalWords);
					}					
					else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) {
						vectorPattern = CreateWord2VecVectors.createVecCentroid(relationalWords);
					}				
				}				
				vector.addi(vectorPattern);
			}
			
			if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) {
				vector = vector.div((float) patterns.size()); 
			}									

			Set<WordEntry> similar_words = BREDSConfig.word2vec.distance(vector.data, BREDSConfig.top_k);
			
			for (WordEntry wordEntry : similar_words) {
				System.out.println(wordEntry.name);
				words.add(wordEntry.name);
			}
			System.out.println();
		}
		
		return words;
	}
	
	
	
	
	
	static LinkedList<BREDSTuple> matchSeedsTuples(List<BREDSTuple> processedTuples) {
		
		Map<Seed,Integer> counts = new HashMap<Seed, Integer>();
		LinkedList<BREDSTuple> matchedTuples = new LinkedList<>();
		int processed = 0;
		for (BREDSTuple tuple : processedTuples) {
			if (processed % 10000==0) System.out.println(processed + " of " + processedTuples.size());
			for (Seed seed : BREDSConfig.seedTuples) {
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
		for (Seed s : BREDSConfig.seedTuples) if (counts.get(s) == null) System.out.println(s.e1 + '\t' + s.e2 + "\t 0 tuples");
		
		return matchedTuples;
	}
	
	static void generateTuples(String file, List<BREDSTuple> processedTuples) throws Exception {
		String sentence = null;
		String e1_begin = "<"+BREDSConfig.e1_type+">";
		String e1_end = "</"+BREDSConfig.e1_type+">";
		String e2_begin = "<"+BREDSConfig.e2_type+">";
		String e2_end = "</"+BREDSConfig.e2_type+">";		
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
			List<Pair<Integer,Integer>> allMatches1 = new ArrayList<Pair<Integer,Integer>>();
			List<Pair<Integer,Integer>> allMatches2 = new ArrayList<Pair<Integer,Integer>>();

			// Find all occurrences of semantic type of e1
			while (matcher1.find()) allMatches1.add(new Pair<Integer,Integer>(matcher1.start(),matcher1.end()));
			
			// Find all occurrences of semantic type of e2
			while (matcher2.find()) allMatches2.add(new Pair<Integer,Integer>(matcher2.start(),matcher2.end()));
			
			for (Pair<Integer, Integer> pair1 : allMatches1) {
				for (Pair<Integer, Integer> pair2 : allMatches2) {					
					String e1 = (sentence.substring(pair1.getFirst(),pair1.getSecond())).replaceAll("<[^>]+>"," ");
					String e2 = (sentence.substring(pair2.getFirst(),pair2.getSecond())).replaceAll("<[^>]+>"," ");
					
					// do not consider cases where e2 occurs before e1
					if ( (!BREDSConfig.e1_type.equals(BREDSConfig.e2_type) && pair2.getSecond()<pair1.getFirst() || pair2.getFirst()<pair1.getSecond())) continue;
					
					// consider the case where the e1 occurs before e2
					if ( pair1.getSecond()<pair2.getFirst()) {
						
						// Ignore contexts where another entity occur between the two entities
						String middleText = sentence.substring(pair1.getSecond(),pair2.getFirst());
	            		Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
	            		Matcher matcher = ptr.matcher(middleText);            		
		            	if (!matcher.find()) {
		            		
							// Consider only tokens, name-entities are not part of the considered vocabulary               		
			            	String left_txt = sentence.substring(0,pair1.getFirst()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
			            	String middle_txt = sentence.substring(pair1.getSecond(),pair2.getFirst()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
			            	String right_txt = sentence.substring(pair2.getSecond()+1).replaceAll("<[^>]+>[^<]+</[^>]+>","");
			        		String[] middle_tokens = middle_txt.trim().split("\\s+");
			        		
			        		// if number of tokens between entities is within the specified limits create a Tuple
			                if (middle_tokens.length<=BREDSConfig.max_tokens_away && middle_tokens.length>=BREDSConfig.min_tokens_away) {	                	
			                	
			        			BREDSTuple t = new BREDSTuple(left_txt, middle_txt, right_txt, e1.trim(), e2.trim(), sentence);	        			
			        			processedTuples.add(t);        			
			                }
		            	}
					}					
				}				
			}
		count++;
		}
		f1.close();
	}
	
	/*
	 * Calculates the confidence of a tuple is: Conf(P_i) * DegreeMatch(P_i)
	 */	
	static void calculateTupleConfidence(Map<BREDSTuple, List<Pair<BREDSPattern, Double>>> candidateTuples) throws IOException {
		
		for (BREDSTuple t : candidateTuples.keySet()) {
			double confidence = 1;
			if (iter>0) t.confidence_old = t.confidence; 
			for (Pair<BREDSPattern, Double> pair : candidateTuples.get(t)) {
				confidence *= ( 1 - (pair.getFirst().confidence() * pair.getSecond()) );
			}
			t.confidence = 1 - confidence;
			// If tuple was already seen use past confidence values to calculate new confidence 
			if (iter>0) {
				t.confidence = t.confidence * BREDSConfig.wUpdt + t.confidence_old * (1 - BREDSConfig.wUpdt);
			}
		}
	}
}







