package bin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import tuples.Seed;
import tuples.Tuple;
import utils.Pair;
import utils.SortMaps;
import vsm.TermsVector;
import clustering.Singlepass;
import clustering.SnowballPattern;
import clustering.dbscan.CosineMeasure;

public class Main {
	
	static int iter = 0;
	
	public static void main(String[] args) throws Exception {
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + " Mbytes");
		long startTime = System.nanoTime();

		if (args.length==0) {
			System.out.println("java -jar snowbal.jar sentencesFile word2vec|tfidf stopwords parameters.cfg seedsFile word2vecmodelpath");
			System.out.println();
			System.exit(0);
		}
		
		/* Read configuration files, sentence files, initial seeds files */
		String sentencesFile = args[0];
		String vectors = args[1];
		String stopwords = args[2];
		String parameters = args[3];
		String seedsFile = args[4];
		String word2vecmodelPath = args[5];
		Config.init(parameters, sentencesFile, stopwords, vectors, word2vecmodelPath);
		Config.readSeeds(seedsFile);			
		if (Config.e1_type==null || Config.e2_type==null) {
			System.out.println("No semantic types defined");
		}
		Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples = new HashMap<Tuple, List<Pair<SnowballPattern,Double>>>();
		LinkedList<SnowballPattern> patterns = new LinkedList<SnowballPattern>();
				
		// Print configuration parameters values to screen
		System.out.println();
		for (String p : Config.parameters.keySet()) System.out.println(p + '\t' + Config.parameters.get(p));
		
		/* 
		 * Starts REDS extraction process
		 */ 
		
		if (Config.REDS==true) {
			REDS.start(sentencesFile,seedsFile,candidateTuples,patterns);
			outputToFiles(candidateTuples, patterns);
			System.exit(0);
		}
		
		/*
		 *  Starts a Snowball extraction process
		 */		
		else if (Config.REDS==false) {
			Snowball.start(sentencesFile,seedsFile,candidateTuples,patterns);
			outputToFiles(candidateTuples, patterns);
			System.exit(0);
		}
		
		
		// start iterative process		
		//iteration(startTime, sentencesFile,candidateTuples,patterns,tuples);
	}

	static void iteration(long startTime, String sentencesFile, Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, LinkedList<Tuple> tuples) throws IOException, Exception {					
				
		while (iter<=Config.parameters.get("number_iterations")) {
			
			// Collect sentences where both entities occur
			// Construct a Tuple object for each 
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Collecting tuples acording to " +  Config.seedTuples.size() + " seeds ");			
			tuples = gatherSentences(sentencesFile,Config.seedTuples);			
			if (tuples.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}			
			else {
				System.out.println("\nClustering tuples ...");
				Singlepass.singlePass(tuples, patterns);				
				System.out.println("\n"+patterns.size() + " patterns generated");
				
				/*
				for (SnowballPattern p : patterns) {
					System.out.println("Confidence	:" + p.confidence);
					System.out.println("#Tuples		:" + p.tuples.size());
					for (Tuple tuple : p.tuples) {	
						System.out.println("Left: ");
						System.out.println(tuple.left_words);
						System.out.println();
						System.out.println("Middle: ");
						System.out.println(tuple.middle_words);
						System.out.println(tuple.patterns);
						System.out.println();
						System.out.println("Right: ");
						System.out.println(tuple.right_words);
						System.out.println();
						System.out.println();
					}
					System.out.println("============================================");
				}
				System.out.println();
				*/	
				
		        // Eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<SnowballPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					SnowballPattern p = patternIter.next();
					if ((p.tuples.size()<Config.parameters.get("min_pattern_support"))) patternIter.remove();
				}
				patternIter = null;
				System.out.println(patterns.size() + " patterns supported by at least " + Config.parameters.get("min_pattern_support") + " tuple(s)");

				//TODO: Expand extraction patterns
				//expandPatterns(patterns);				
				

				// - Look for sentences with occurrence of seeds semantic type (e.g., ORG - LOC)
				// - Measure the similarity of each sentence with each Pattern
				// - Matching tuples are also used to score patterns confidence, based on being correct
				//   or not according to the seed set
				
				System.out.println("Collecting " + Config.e1_type + " - " + Config.e2_type + " sentences and computing similarity with patterns");				
				generateTuples(candidateTuples,patterns,sentencesFile);				
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
		outputToFiles(candidateTuples,patterns);
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
		double eps = 0.3;
		int minPts = 2;
		DBSCANClusterer<Clusterable> dbscan = new DBSCANClusterer<>(eps, minPts, measure);

		// Tuples implement the Clusterable Interface to allow clustering
		// Add all tuples to a Clusterable collection to be passed to DBSCAN
		LinkedList<Clusterable> points = new LinkedList<Clusterable>();
				
		for (Tuple t : tuples) {			
			// Cluster according to ReVerb patterns
			// TODO: tuples com mais do que um padrão ReVerb ?
			if (t.ReVerbpatterns.size()>0) {
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
				System.out.println(t.ReVerbpatterns);
			}
			patterns.add(pattern);
			System.out.println();
		}
		c++;
	}
	

	private static double compactness(List<Clusterable> objects){
		CosineMeasure ms = new CosineMeasure();
		double avg = 0;
		for (Clusterable obj1 : objects) {
			for (Clusterable obj2 : objects) {
				if (!obj1.equals(obj2)) {
					double[] a = obj1.getPoint();
					double[] b = obj2.getPoint();					
					avg += ms.compute(a, b);
				}
			}			
		}
		return (double) (avg / (double) objects.size());
	}

	
	/*
	 * Writes the extracted Tuples and the generated Extraction Patterns to files
	 */ 
	static void outputToFiles(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException {
		BufferedWriter f1 = new BufferedWriter(new FileWriter("tuples.txt"));
		BufferedWriter f2 = new BufferedWriter(new FileWriter("patterns.txt"));
		ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
		Collections.sort(tuplesOrdered);
		Collections.reverse(tuplesOrdered);
		for (Tuple t : tuplesOrdered) {
			f1.write("tuple:" + t.e1 + '\t' + t.e2 + '\t' + t.confidence + "\n");
			f1.write(t.sentence + "\n\n");
		}
		f1.close();
		if (Config.useWord2Vec==true) {
			for (SnowballPattern p : patterns)
				f2.write(String.valueOf(p.confidence) + '\t' + p.left_centroid + '\t' + p.middle_centroid + '\t' + p.right_centroid + '\n');
			f2.close();			
		}
		else {			
			if (Config.REDS==false) {
				for (SnowballPattern p : patterns) {
					f2.write("confidence	:" + p.confidence+'\n');
					f2.write("#tuples		:" + p.tuples.size()+'\n');
					for (Tuple tuple : p.tuples) {
						f2.write("\nleft: ");
						for (String word : tuple.left_words) f2.write(word+',');
						f2.write("\nmiddle: ");
						for (String word : tuple.middle_words) f2.write(word+',');
						f2.write("\nright: ");
						for (String word : tuple.right_words) f2.write(word+',');
						f2.write("\n");
					}
					f2.write("\n================================================\n");
				}				
			}
			else if (Config.REDS==true){
				for (SnowballPattern p : patterns) {
					f2.write("confidence	:" + p.confidence+'\n');
					f2.write("#tuples		:" + p.tuples.size()+'\n');
					f2.write("patterns		:" + p.patterns+'\n');
					f2.write("\n================================================\n");				
				}				
			}			
			f2.close();						
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
				t.confidence = t.confidence * Config.parameters.get("wUpdt") + t.confidence_old * (1 - Config.parameters.get("wUpdt"));
			}
		}
	}


	/*
	 * Look for sentences with occurrence of seeds semantic type (i.e., ORG - LOC)
	 * Measure the similarity of each sentence with each Pattern
	 */  
	
	static void generateTuples(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, String file) throws Exception {
		String sentence = null;
		String e1_begin = "<"+Config.e1_type+">";
		String e1_end = "</"+Config.e1_type+">";
		String e2_begin = "<"+Config.e2_type+">";
		String e2_end = "</"+Config.e2_type+">";
		double maxRlogF = 0;
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
	                	
	                if (middle_t.size()<=Config.parameters.get("max_tokens_away") && middle_t.size()>=Config.parameters.get("min_tokens_away") && middle_t.size()>0) {
	                	
	                	// Create a Tuple for an occurrence found
	        			Tuple t = new Tuple(left_t, middle_t, right_t, e1, e2, sentence, middle_txt);        				
	        			double simBest = 0;
	        			SnowballPattern patternBest = null;
	        			List<Integer> patternsMatched = new LinkedList<Integer>();
	        			
	        			// Compute similarity with all the Patterns
	        			
	        			//TODO: compare using ReVerb patterns
	        			/*
        				if (Config.useReverb==true && Config.useCentroid==false && Config.useSum==false) {
	        				// Compare the ReVerb patterns/middle words with every extraction pattern (clusters of sentences/tuples)
        					for (Tuple w : p.tuples) {
								FloatMatrix a = w.patternsWord2Vec.get(0);
								FloatMatrix b = t.patternsWord2Vec.get(0);
								score = TermsVector.cosSimilarity(a, b); 
								if (score>=max){
									max = score;
								}
        					}
        				}
    					*/
	        			
        				simBest = Double.NEGATIVE_INFINITY;
	        			
        				
        				
        				for (SnowballPattern pattern : patterns) {
        					Double similarity = null;       					
        					
            			    /*
            				 *  Compare using word2vec representations   
            				 *  Compare each sentence/Tuple with a Pattern centroid according to a Word2Vec representation
            				 *  this is independent whether the Sum or Centroid was used to generate the context vectors   
            			     *  if for each cluster the maximum is >= threshold, sentence/tuple is collected   
            			     */
        					if ( Config.useWord2Vec==true ) {
        						if (Config.useSum==true) {
		        					similarity = t.degreeMatchWord2VecSum(pattern.w2v_left_centroid, pattern.w2v_middle_centroid, pattern.w2v_right_centroid);		        				
		        				}
			  					else if (Config.useCentroid==true) {			  						
 			  						similarity = t.degreeMatchWord2VecCentroid(pattern.w2v_left_centroid, pattern.w2v_middle_centroid, pattern.w2v_right_centroid);
			  					}
        					}
        					
        					/*
        					 * Compare using the TF-IDF representations
        					 */
        					else if ( Config.useWord2Vec==false ) {
        						similarity = t.degreeMatchCosTFIDF(pattern.left_centroid, pattern.middle_centroid, pattern.right_centroid);        						
        					}
        					
	        				// If the similarity between the sentence where the tuple was extracted and a 
	        				// pattern is greater than a threshold update the pattern confidence
        					
        					if (similarity>=Config.parameters.get("min_degree_match")) {
	        					patternsMatched.add(patterns.indexOf(pattern));
	        					pattern.updatePatternSelectivity(e1,e2);
	        					if (iter>0) {
	        						pattern.confidence_old = pattern.confidence;	        						
	        						pattern.RlogF_old = pattern.RlogF;
	        					}
	        					pattern.confidence();        						
	        					if (Config.parameters.get("use_RlogF")==1) {	        						
	        						pattern.ConfidencePatternRlogF();
	        					}
	        					if (similarity>=simBest) {
	        						simBest = similarity;
	        						patternBest = pattern;    
	        					}
	        				}
	        			}
	        				
        				// RlogF needs to be normalized: [0,1]
	        			if (Config.parameters.get("use_RlogF")==1) {	        				
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
						if (simBest>=Config.parameters.get("min_degree_match")) {
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
	 * Collects sentences based on seeds instances 
	 */
	static LinkedList<Tuple> gatherSentences(String sentencesFile, Set<Seed> seeds) throws IOException{
		String e1_begin = "<"+Config.e1_type+">";
		String e1_end = "</"+Config.e1_type+">";
		String e2_begin = "<"+Config.e2_type+">";
		String e2_end = "</"+Config.e2_type+">";		
		Map<Seed,Integer> counts = new HashMap<Seed, Integer>();
		LinkedList<Tuple> tuples = new LinkedList<Tuple>();
		List<String> left = null;
		List<String> right = null;
		List<String> middle = null;
		String sentence = null;
		/*
		String date = null;
		Integer sentence_id = null;
		Integer url_id = null;
		*/
		Tuple t = null;
    	int n_lines = 0;
		BufferedReader f = new BufferedReader(new FileReader(new File(sentencesFile)));	   	    
	    while ( ( sentence = f.readLine() ) != null ) {
			if (n_lines % 10000 == 0) System.out.print(".");			
			sentence = sentence.trim();
			try {
				for (Seed seed : seeds) {
	            	Pattern pattern1 = Pattern.compile(e1_begin+seed.e1+e1_end);
	        		Pattern pattern2 = Pattern.compile(e2_begin+seed.e2+e2_end);
	            	Matcher matcher1 = pattern1.matcher(sentence);
	            	Matcher matcher2 = pattern2.matcher(sentence);
	            	
	            	if (matcher1.find() && matcher2.find()) {            		
	            		matcher1 = pattern1.matcher(sentence);
	            		matcher2 = pattern2.matcher(sentence);                          
						matcher1.find();
						matcher2.find();					
	            		if (matcher2.end()<matcher1.end() || matcher2.start()<matcher1.end()) continue;
		            		           			            		
	            		// some restriction over selected sentences:            		
	            		// ignore contexts where another entity occur between the two entities
	            		//String middleText = sentence.substring(matcher1.end(),matcher2.start());
	            		//Pattern pattern = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
	            		//Matcher matcher = pattern.matcher(middleText);            		
	            		//if (matcher.find()) continue;
	            		
	            		// Ignore contexts where another entity occur between the two entities	    					
	    				String middleText = sentence.substring(matcher1.end(),matcher2.start());
	                	Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
	                	Matcher matcher = ptr.matcher(middleText);            		
	    	            if (matcher.find()) continue;
	    	            	
	            		// Split the sentence into three contexts: BEF, BET, AFT                		
	            		String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		String right_txt = sentence.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		
	            		// Normalize text, convert to to lower case, remove stop words
	            		left = TermsVector.normalize(left_txt);				
	        			middle = TermsVector.normalize(middle_txt);
						right = TermsVector.normalize(right_txt);
	                	
	                	if (middle.size()<=Config.parameters.get("max_tokens_away") && middle.size()>=Config.parameters.get("min_tokens_away") && middle.size()>0) {
		                	
	                		// Generate a Tuple with TF-IDF vectors and Word2Vec vectors	                		
	                		t = new Tuple(left, middle, right, seed.e1, seed.e2, sentence, middle_txt);
	                		tuples.add(t);	                			        				
	                		Integer count = counts.get(seed);
	        				if (count==null) counts.put(seed, 1);
	        				else counts.put(seed, ++count);
		            	}
			    	}
			    }
			} catch (Exception e) {
					//System.out.println(sentence);				
			}            
        n_lines++;
	    }
	    f.close();
	    System.out.println();
	    /* print the seed with number of occurrences per seed sorted by descending order */
		ArrayList<Map.Entry<Seed,Integer>> myArrayList = new ArrayList<Map.Entry<Seed, Integer>>(counts.entrySet());		
		Collections.sort(myArrayList, new SortMaps.StringIntegerComparator());		
		Iterator<Entry<Seed, Integer>> itr=myArrayList.iterator();
		Seed key=null;
		int value=0;
		int cnt=0;
		while(itr.hasNext()){	
			cnt++;
			Map.Entry<Seed,Integer> e = itr.next();		
			key = e.getKey();
			value = e.getValue().intValue();		
			System.out.println(key.e1 + '\t'+ key.e2 +"\t" + value);
		}
		for (Seed s : Config.seedTuples) if (counts.get(s) == null) System.out.println(s.e1 + '\t' + s.e2 + "\t 0 tuples");
	    return tuples;
	}
}