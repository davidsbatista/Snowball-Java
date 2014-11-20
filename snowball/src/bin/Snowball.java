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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.Stopwords;

import org.apache.commons.lang3.StringUtils;

import tuples.Seed;
import tuples.SnowballTuple;
import utils.Pair;
import utils.SortMaps;
import vsm.TermsVector;
import clustering.Singlepass;
import clustering.SnowballPattern;

public class Snowball {
	
	public static int iter = 0;

	public static void start(String sentencesFile, String seedsFile,Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException, Exception {		
		long startTime = System.nanoTime();
		SnowballConfig.readSeeds(seedsFile);
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	static void iteration(long startTime, String sentencesFile, Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException, Exception {					

		List<SnowballTuple> processedTuples = new LinkedList<SnowballTuple>();		
		File f = new File("Snowball_processed_tuples.obj");
		
		if (!f.exists()) {
			System.out.println("\nPre-processing data");
			Snowball.generateTuples(sentencesFile,processedTuples);
			System.out.println("\n"+processedTuples.size() + " tuples gathered");
			
			try {
				// Save to disk
				FileOutputStream out = new FileOutputStream("Snowball_processed_tuples.obj");
				ObjectOutputStream oo = new ObjectOutputStream(out);
				oo.writeObject(processedTuples);
				oo.close();
			} catch (Exception e) { 
				e.printStackTrace();
				System.exit(0);
			}
		}
		else {
			// Load from disk
			System.out.println("Loading pre-processed sentences");
			FileInputStream in = new FileInputStream("Snowball_processed_tuples.obj");
			ObjectInputStream objectInput = new ObjectInputStream(in);
			processedTuples = (List<SnowballTuple>) objectInput.readObject();
			System.out.println("\n"+processedTuples.size() + " tuples gathered");						
			in.close();
		}
		
		while (iter<=SnowballConfig.number_iterations) {			
			// Collect sentences where both entities occur
			// Construct a Tuple object for each 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Collecting tuples acording to " +  SnowballConfig.seedTuples.size() + " seeds ");			
			LinkedList<SnowballTuple> seedMatches = matchSeedsTuples(processedTuples);
			
			if (iter==0) {
				System.out.println("Seed matches: " + seedMatches.size());
				for (SnowballTuple tuple : seedMatches) {
					System.out.println(tuple.sentence);
					System.out.println("left: " + tuple.left);
					System.out.println("middle: " + tuple.middle);
					System.out.println("right: " + tuple.right);
					System.out.println();
				}				
			}
			System.out.println();
			
			if (seedMatches.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			else {
				System.out.println("\nClustering tuples ...");
				Singlepass.singlePassTFIDF(seedMatches, patterns);				
				System.out.println("\n"+patterns.size() + " patterns generated");
				
		        // Eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<SnowballPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					SnowballPattern p = patternIter.next();
					if (p.tuples.size()<SnowballConfig.min_pattern_support) patternIter.remove();
				}
				patternIter = null;
				
				System.out.println(patterns.size() + " patterns supported by at least " + SnowballConfig.min_pattern_support + " tuple(s)");
				
				System.out.println("Patterns " + patterns.size() + " generated");
				for (SnowballPattern p: patterns) {
					System.out.println("confidence	:" + p.confidence);
					System.out.println("#tuples		:" + p.tuples.size());
					for (SnowballTuple t: p.tuples) {
						System.out.println("left 	:" + t.left.keySet());
						System.out.println("middle 	:" + t.middle.keySet());
						System.out.println("right	:" + t.right.keySet());
						System.out.println();
					}
					System.out.println("====================================\n");
				}
			

				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence with each Pattern
				// - Matching tuples are also used to score patterns confidence, based on being correct
				//   or not according to the seed set				
				System.out.println("Collecting " + SnowballConfig.e1_type + " - " + SnowballConfig.e2_type + " sentences and computing similarity with patterns");
				comparePatternsTuples(candidateTuples, patterns, processedTuples);
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				// Update Tuple confidence based on patterns confidence
				System.out.println("Calculating tuples confidence");
				calculateTupleConfidence(candidateTuples);
				
				System.out.println("\n"+candidateTuples.size() + " tuples");

				// Print each collected Tuple and its confidence				
				ArrayList<SnowballTuple> tuplesOrdered  = new ArrayList<SnowballTuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (SnowballTuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
				
				// Calculate a new seed set of tuples to use in next iteration, such that:
				// seeds = { T | Conf(T) > min_tuple_confidence }
				System.out.println("Adding tuples with confidence =>" + SnowballConfig.min_tuple_confidence + " as seed for next iteration");
				int added = 0;
				int removed = 0;				
				for (SnowballTuple t : candidateTuples.keySet()) {
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

	static void comparePatternsTuples(Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns, List<SnowballTuple> processedTuples) {		
		int count = 0;		
		for (SnowballTuple t : processedTuples) {
			if (count % 10000==0) System.out.print(".");
			List<Integer> patternsMatched = new LinkedList<Integer>();
			double simBest = 0;
			double maxRlogF = 0;
			SnowballPattern patternBest = null;			
			simBest = Double.NEGATIVE_INFINITY;
			// Compute similarity with all the extraction patterns using the TF-IDF representations 
    		for (SnowballPattern pattern : patterns) {    				 
				double similarity = t.degreeMatchCosTFIDF(pattern.left_centroid, pattern.middle_centroid, pattern.right_centroid);				
				// If the similarity between the sentence where the tuple was extracted and a 
    			// pattern is greater than a threshold update the pattern confidence					
				if (similarity>=SnowballConfig.min_degree_match) {
    				patternsMatched.add(patterns.indexOf(pattern));
    				pattern.updatePatternSelectivity(t.e1,t.e2);
    				if (iter>0) {
    					pattern.confidence_old = pattern.confidence;	        						
    					pattern.RlogF_old = pattern.RlogF;
    				}
    				pattern.confidence();        						
    				if (SnowballConfig.use_RlogF=true) {	        						
    					pattern.ConfidencePatternRlogF();
    				}
    				if (similarity>=simBest) {
    					simBest = similarity;
    					patternBest = pattern;    
    				}
    			}
    		}
    				
			// RlogF needs to be normalized: [0,1]
			if (SnowballConfig.use_RlogF=true) {	        				
				// Find maximum confidence value
				for (SnowballPattern p : patterns) {
					if (p.RlogF>maxRlogF) maxRlogF=p.RlogF;
				}	        				
				// Normalize
				for (Integer integer : patternsMatched) {
					SnowballPattern p = patterns.get(integer);
					if (p.RlogF>0) p.RlogF = p.RlogF / maxRlogF;
					else p.RlogF = 0;
				}			
			}
					
			/*
			 * Associate highest scoring pattern with the Tuple
			 * Create a Pair object with the Pattern and the similarity score to the tuple that it matched  
			 */
			if (simBest>=SnowballConfig.min_degree_match) {
				List<Pair<SnowballPattern, Double>> list = null;
				Pair<SnowballPattern,Double> p = new Pair<SnowballPattern, Double>(patternBest, simBest);

				// Check if the tuple was already extracted in a previous iteration
				SnowballTuple tupleInCandidatesMap = null;	        					
				for (SnowballTuple extractedT : candidateTuples.keySet()) {
					if (t.equals(extractedT)) {
						tupleInCandidatesMap = extractedT;        							
					}        						
				}
				
				// If the tuple was not seen before:
				//  - associate it with this Pattern and similarity score 
				//  - add it to the list of candidate Tuples
				if ( tupleInCandidatesMap == null ) {
					list = new LinkedList<Pair<SnowballPattern,Double>>();        						
					list.add(p);
					tupleInCandidatesMap = t;
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
			count++;
		}
	}
	
	/*
	 * Calculates the confidence of a tuple is: Conf(P_i) * DegreeMatch(P_i)
	 */	
	static void calculateTupleConfidence(Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples) throws IOException {
		for (SnowballTuple t : candidateTuples.keySet()) {			
			double confidence = 1;
			if (iter>0) {
				t.confidence_old = t.confidence;
			}
			List<Pair<SnowballPattern, Double>> listPatterns = candidateTuples.get(t);
			for (Pair<SnowballPattern, Double> pair : listPatterns) {
				confidence *= ( 1 - (pair.getFirst().confidence() * pair.getSecond()) );
			}
			t.confidence = 1 - confidence;
			// If tuple was already seen use past confidence values to calculate new confidence 
			if (iter>0) {
				t.confidence = t.confidence * SnowballConfig.wUpdt + t.confidence_old * (1 - SnowballConfig.wUpdt);
			}
		}
	}
	
	static void generateTuples(String file, List<SnowballTuple> processedTuples) throws Exception {
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
					if ( (!SnowballConfig.e1_type.equals(SnowballConfig.e2_type) && pair2.getSecond()<pair1.getFirst() || pair2.getFirst()<pair1.getSecond())) continue;
					
					// consider the case where the e1 occurs before e2
					if ( pair1.getSecond()<pair2.getFirst()) {
						
						// Ignore contexts where another entity occur between the two entities
						String middleText = sentence.substring(pair1.getSecond(),pair2.getFirst());
	            		Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
	            		Matcher matcher = ptr.matcher(middleText);            		
		            	if (!matcher.find()) {
		            		
							// Consider only tokens, name-entities are not part of the ocabulary               		
			            	String left_txt = sentence.substring(0,pair1.getFirst()).replaceAll("<[^>]+>[^<]+</[^>]+> ","");
			            	String middle_txt = sentence.substring(pair1.getSecond(),pair2.getFirst()).replaceAll("<[^>]+>[^<]+</[^>]+> ","");
			            	String right_txt = sentence.substring(pair2.getSecond()+1).replaceAll("<[^>]+>[^<]+</[^>]+> ","");
			        		String[] middle_tokens = middle_txt.trim().split("\\s");
			        		
			        		// if number of tokens between entities is within the specified limits create a Tuple
			                if (middle_tokens.length<=SnowballConfig.max_tokens_away && middle_tokens.length>=SnowballConfig.min_tokens_away) {
			                	
			                	// Create a Tuple for an occurrence found			                	
			            	    List<String> left =  TermsVector.normalize(getLeftContext(left_txt));
			            	    List<String> middle =  TermsVector.normalize(middle_txt);			            	    
			            	    List<String> right =  TermsVector.normalize(getRightContext(right_txt));
			                	
			                	if (!(left.size()==0 && middle.size()==0 && right.size()==0)) {			            	    	
			            	    	SnowballTuple t = new SnowballTuple(left, middle, right, e1.trim(), e2.trim(), sentence);
				        			processedTuples.add(t);        			
			            	    }
			                }
		            	}
					}					
				}				
			}
		count++;
		}
		f1.close();
	}
	
	public static String getLeftContext(String left){		
		String[] left_tokens = left.split("\\s");
		List<String> tokens = new LinkedList<String>();
		if (left_tokens.length>=SnowballConfig.context_window_size) {
			for (int i = left_tokens.length-1; i > left_tokens.length-1-SnowballConfig.context_window_size; i--) tokens.add(left_tokens[i]);
		} else return left;
		String left_context = StringUtils.join(tokens," ");
		return left_context;
		
	}
	
	public static String getRightContext(String right){
		String[] right_tokens = right.split("\\s");
		List<String> tokens = new LinkedList<String>();		
		if (right_tokens.length>=SnowballConfig.context_window_size) {
			for (int i = 0; i < SnowballConfig.context_window_size; i++) {
				tokens.add(right_tokens[i]);
			}
		} else return right;
		String right_context = StringUtils.join(tokens," ");
		return right_context;
	}
	
	
	
	

	static LinkedList<SnowballTuple> matchSeedsTuples(List<SnowballTuple> processedTuples) {
		
		Map<Seed,Integer> counts = new HashMap<Seed, Integer>();
		LinkedList<SnowballTuple> matchedTuples = new LinkedList<>();
		int processed = 0;
		for (SnowballTuple tuple : processedTuples) {
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
		for (Seed s : SnowballConfig.seedTuples) if (counts.get(s) == null) System.out.println(s.e1 + '\t' + s.e2 + "\t 0 tuples");
		return matchedTuples;
	}
}
