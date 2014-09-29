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

import tuples.Seed;
import tuples.Tuple;
import utils.Pair;
import utils.SortMaps;
import vsm.TermsVector;
import clustering.Singlepass;
import clustering.SnowballPattern;

public class Snowball {
	
	public static int iter = 0;

	public static void start(String sentencesFile, String seedsFile,Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException, Exception {		
		long startTime = System.nanoTime();
		Config.readSeeds(seedsFile);
		iteration(startTime, sentencesFile, candidateTuples, patterns);		
	}
	
	static void iteration(long startTime, String sentencesFile, Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException, Exception {					

		Set<Tuple> processedTuples = new HashSet<Tuple>();		
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
			// Load
			System.out.println("Loading pre-processed sentences");
			FileInputStream in = new FileInputStream("Snowball_processed_tuples.obj");
			ObjectInputStream objectInput = new ObjectInputStream(in);
			processedTuples = (Set<Tuple>) objectInput.readObject();
			System.out.println("\n"+processedTuples.size() + " tuples gathered");						
			in.close();
		}
		
		while (iter<=Config.number_iterations) {			
			// Collect sentences where both entities occur
			// Construct a Tuple object for each 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Collecting tuples acording to " +  Config.seedTuples.size() + " seeds ");			
			LinkedList<Tuple> seedMatches = matchSeedsTuples(processedTuples);			
			if (seedMatches.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			else {
				System.out.println("\nClustering tuples ...");
				Singlepass.singlePass(seedMatches, patterns);				
				System.out.println("\n"+patterns.size() + " patterns generated");
				
		        // Eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<SnowballPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					SnowballPattern p = patternIter.next();
					if (p.tuples.size()<Config.min_pattern_support) patternIter.remove();
				}
				patternIter = null;
				System.out.println(patterns.size() + " patterns supported by at least " + Config.min_pattern_support + " tuple(s)");

				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence with each Pattern
				// - Matching tuples are also used to score patterns confidence, based on being correct
				//   or not according to the seed set				
				System.out.println("Collecting " + Config.e1_type + " - " + Config.e2_type + " sentences and computing similarity with patterns");
				comparePatternsTuples(candidateTuples, patterns, processedTuples);
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				
				System.out.println("Patterns " + patterns.size() + " generated");
				for (SnowballPattern p: patterns) {
					System.out.println("confidence	:" + p.confidence);
					System.out.println("#tuples		:" + p.tuples.size());
					for (Tuple t: p.tuples) {
						//System.out.println("left 	:" + t.left_words);
						System.out.println("middle 	:" + t.middle_words);
						//System.out.println("right	:" + t.right_words);
						System.out.println();
					}
					System.out.println("====================================\n");
				}
				
				// Update Tuple confidence based on patterns confidence
				System.out.println("Calculating tuples confidence");
				calculateTupleConfidence(candidateTuples);
				
				System.out.println("\n"+candidateTuples.size() + " tuples");

				// Print each collected Tuple and its confidence				
				ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (Tuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
				
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

	static void comparePatternsTuples(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, Set<Tuple> processedTuples) {
		
		for (Tuple t : processedTuples) {
			// Compute similarity with all the extraction patterns
			List<Integer> patternsMatched = new LinkedList<Integer>();
			double simBest = 0;
			double maxRlogF = 0;
			SnowballPattern patternBest = null;			
			simBest = Double.NEGATIVE_INFINITY;
						
    		for (SnowballPattern pattern : patterns) {    				
    			//Compare using the TF-IDF representations
				double similarity = t.degreeMatchCosTFIDF(pattern.left_centroid, pattern.middle_centroid, pattern.right_centroid);
				
				// If the similarity between the sentence where the tuple was extracted and a 
    			// pattern is greater than a threshold update the pattern confidence					
				if (similarity>=Config.min_degree_match) {
    				patternsMatched.add(patterns.indexOf(pattern));
    				pattern.updatePatternSelectivity(t.e1,t.e2);
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
    				
			// RlogF needs to be normalized: [0,1]
			if (Config.use_RlogF=true) {	        				
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
			if (simBest>=Config.min_degree_match) {
				List<Pair<SnowballPattern, Double>> list = null;
				Pair<SnowballPattern,Double> p = new Pair<SnowballPattern, Double>(patternBest, simBest);

				// Check if the tuple was already extracted in a previous iteration
				Tuple tupleInCandidatesMap = null;	        					
				for (Tuple extractedT : candidateTuples.keySet()) {
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
		}
	}
	
	/*
	 * Calculates the confidence of a tuple is: Conf(P_i) * DegreeMatch(P_i)
	 */	
	static void calculateTupleConfidence(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples) throws IOException {
		for (Tuple t : candidateTuples.keySet()) {			
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
				t.confidence = t.confidence * Config.wUpdt + t.confidence_old * (1 - Config.wUpdt);
			}
		}
	}
	
	static void generateTuples(String file, Set<Tuple> processedTuples) throws Exception {
		String sentence = null;
		String e1_begin = "<"+Config.e1_type+">";
		String e1_end = "</"+Config.e1_type+">";
		String e2_begin = "<"+Config.e2_type+">";
		String e2_end = "</"+Config.e2_type+">";
		List<String> left_t = null;
		List<String> middle_t = null;
		List<String> right_t = null;		
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
			if (Config.e1_type.equals(Config.e2_type) && found1) {
				found2 = matcher2.find();
			}		
			
			try {
				String e1 = (sentence.substring(matcher1.start(),matcher1.end())).replaceAll("<[^>]+>"," ");
				String e2 = (sentence.substring(matcher2.start(),matcher2.end())).replaceAll("<[^>]+>"," ");
				if ( (!Config.e1_type.equals(Config.e2_type) && matcher2.end()<matcher1.end() || matcher2.start()<matcher1.end())) continue;
								
				if ( (found1 && found2) && matcher1.end()<matcher2.end()) {
					
					// Ignore contexts where another entity occur between the two entities
					String middleText = sentence.substring(matcher1.end(),matcher2.start());
            		Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
            		Matcher matcher = ptr.matcher(middleText);            		
	            	if (matcher.find()) continue;
	            	
					// Constructs vectors considering only tokens, name-entities are not part of the vectors               		
	            	String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String right_txt = sentence.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	
	            	left_t = TermsVector.normalize(left_txt);
	        		middle_t = TermsVector.normalize(middle_txt);
					right_t = TermsVector.normalize(right_txt);
	                	
	                if (middle_t.size()<=Config.max_tokens_away && middle_t.size()>=Config.min_tokens_away && middle_t.size()>0) {
	                	
	                	// Create a Tuple for an occurrence found        				
	        			Tuple t = new Tuple(left_t, middle_t, right_t, e1.trim(), e2.trim(), sentence, middle_txt);	        			
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

	static LinkedList<Tuple> matchSeedsTuples(Set<Tuple> processedTuples) {
		
		Map<Seed,Integer> counts = new HashMap<Seed, Integer>();
		LinkedList<Tuple> matchedTuples = new LinkedList<>();
		
		for (Tuple tuple : processedTuples) {
			for (Seed seed : Config.seedTuples) {
				if (tuple.e1.equalsIgnoreCase(seed.e1) && tuple.e2.equalsIgnoreCase(seed.e2)) {
					matchedTuples.add(tuple);					
					Integer count = counts.get(seed);
					if (count==null) counts.put(seed, 1);
					else counts.put(seed, ++count);
				}
			}			
		}
		
		/* Print number of seed matches sorted by descending order */
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
		return matchedTuples;
	}
}
