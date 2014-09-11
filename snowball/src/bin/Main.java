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

import nlp.PortugueseVerbNormalizer;
import tuples.Seed;
import tuples.Tuple;
import utils.Pair;
import utils.SortMaps;
import vsm.TermsVector;
import clustering.Singlepass;
import clustering.SnowballPattern;

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
		
		/* read configuration files, sentence files, initial seeds files */
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
			System.exit(0);
		}
		
		/* TEST WORD2VEC
		System.out.println("Size   			: " + Config.word2vec.getSize());
		System.out.println("Analogy			: " + Config.word2vec.analogy("rei", "homem", "mulher"));
		System.out.println("Word Vector		: " + Config.word2vec.getWordVector("rei"));
		System.out.println("TopNSize		: " + Config.word2vec.getTopNSize());		
		System.out.println("Distance		: " + Config.word2vec.distance("acusou"));		
		System.out.println();

		float[] v1 = Config.word2vec.getWordVector("chefe");
		float[] v2 = Config.word2vec.getWordVector("do");
		
		float[] v3 = Config.word2vec.getWordVector("presidente");
		float[] v4 = Config.word2vec.getWordVector("do");
				
		FloatMatrix m1 =  new FloatMatrix(v1);
		FloatMatrix m2 =  new FloatMatrix(v2);
		
		FloatMatrix m3 =  new FloatMatrix(v3);
		FloatMatrix m4 =  new FloatMatrix(v4);
		
		m1.addi(m2);
		m3.addi(m4);
				
		System.out.println(m1);		
		System.out.println(m3);		
		System.out.println("cos(): " + TermsVector.cosSimilarity(m1,m3));		
		System.exit(0);
		*/
	
		// initialize Stemmer
		// StemmerWrapper.initialize();
		
		// initialize VerbLexicon for normalization */
		PortugueseVerbNormalizer.initialize();
				
		// print parameters values to screen
		for (String p : Config.parameters.keySet()) System.out.println(p + '\t' + Config.parameters.get(p));
		
		// start iterative process
		Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples = new HashMap<Tuple, List<Pair<SnowballPattern,Double>>>();
		LinkedList<SnowballPattern> patterns = new LinkedList<SnowballPattern>();
		LinkedList<Tuple> tuples = null;
		iteration(startTime, sentencesFile,candidateTuples,patterns,tuples);
	}
		
	
	static Tuple getTuple(Tuple t1, Set<Tuple> lst) {
		for(Tuple t2 : lst) {
			if (t1.equals(t2)) return t2;
		}		
		return null;
	}
		
	// starts a Snowball extraction process
	static void iteration(long startTime, String sentencesFile, Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, LinkedList<Tuple> tuples) throws IOException, Exception {					
		
		while (iter<=Config.parameters.get("number_iterations")) {
			
			// gather sentences where both entities occur and and generate a Tuple for each occurrence
			System.out.println("\n***************************");
			System.out.println("Starting iteration " + iter);			
			System.out.println("Generating tuples acording to " +  Config.seedTuples.size() + " seeds ");
			
			tuples = gatherSentences(sentencesFile,Config.seedTuples);
			
			if (tuples.size()==0) {
				System.out.println("No tuples found");
				System.exit(0);	
			}
			
			else {
				Singlepass.singlePass(tuples, patterns);
				
				//patterns = Dbscan.applyDbscan(tuples, patterns);				
				System.out.println("\n"+patterns.size() + " patterns generated");
				
		        // eliminate patterns supported by less than 'min_pattern_support' tuples			
				Iterator<SnowballPattern> patternIter = patterns.iterator();
				while (patternIter.hasNext()) {
					SnowballPattern p = patternIter.next();
					if ((p.tuples.size()<Config.parameters.get("min_pattern_support"))) patternIter.remove();
				}
				patternIter = null;
				System.out.println(patterns.size() + " patterns supported by at least " + Config.parameters.get("min_pattern_support") + " tuple(s)");
				
				System.out.println("Applying patterns to find new instances(tuples) and score patterns confidence");
								
				// find more occurrences using generated patterns, generate tuples from the occurrences 
				// tuples are also used to score patterns confidence
				generateTuples(candidateTuples,patterns,sentencesFile);
				System.out.println("\n"+candidateTuples.size() + " tuples found");
				
				System.out.println("Patterns:");
				for (SnowballPattern p: patterns) {
					System.out.println("confidence	:" + p.confidence);
					System.out.println("#tuples		:" + p.tuples.size());
					for (Tuple t: p.tuples) {
						System.out.println("left 	:" + t.left_words);
						System.out.println("middle 	:" + t.middle_words);
						System.out.println("right	:" + t.right_words);
						System.out.println();
					}					
				}
				
				// update tuple confidence based on patterns confidence
				calculateTupleConfidence(candidateTuples);
								
				// print extracted tuples and confidence of each
				ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
				Collections.sort(tuplesOrdered);
				for (Tuple t : tuplesOrdered) System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
				System.out.println();
								
				// generating new seed set of tuples to use in next iteration: seeds = {T|Conf(T)>min_tuple_confidence}
				System.out.println("Adding tuples with confidence =>" + Config.parameters.get("min_tuple_confidence") + " as seed for next iteration");
				int added=0;
				int removed=0;
				int current_seeds = Config.seedTuples.size();
				for (Tuple t : candidateTuples.keySet()) {
					if (t.confidence>=Config.parameters.get("min_tuple_confidence")) {
						//System.out.println(t.e1 + '\t' + t.e2 + '\t' + t.confidence);
						Config.seedTuples.add(new Seed(t.e1.trim(),t.e2.trim()));
						added++;
					} else removed++;
				}
				if (Config.parameters.get("stop_when_no_new_tuples")==1) {
					if (current_seeds==Config.seedTuples.size()) {
						System.out.println("No new tuples added");
						System.out.println(iter + " iterations done.");
						break;
					}					
				}
				System.out.println(removed + " tuples removed due to confidence lower than " + Config.parameters.get("min_tuple_confidence"));				
				System.out.println(added + " tuples added to seed set");
				iter++;
			}			
		}		
		long stopTime = System.nanoTime();
		long elapsedTime = stopTime - startTime;
		System.out.println("Runtime: " + TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + " seconds");
		System.out.println();
		System.out.println(candidateTuples.size() + " tuples extracted");
		outputToFiles(candidateTuples,patterns);
	}

	// writes the output to files
	static void outputToFiles( Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns) throws IOException {
		BufferedWriter f1 = new BufferedWriter(new FileWriter("tuples.txt"));
		BufferedWriter f2 = new BufferedWriter(new FileWriter("patterns.txt"));
		ArrayList<Tuple> tuplesOrdered  = new ArrayList<Tuple>(candidateTuples.keySet());				
		Collections.sort(tuplesOrdered);
		Collections.reverse(tuplesOrdered);
		for (Tuple t : tuplesOrdered) {
			f1.write("tuple:" + t.e1 + '\t' + t.e2 + '\t' + t.confidence + "\n");
			f1.write(t.date + '\t' + t.url_id + '\t' + t.sentence + "\n\n");
		}
		f1.close();
		for (SnowballPattern p : patterns) 
			f2.write(String.valueOf(p.confidence) + '\t' + p.left_centroid + '\t' + p.middle_centroid + '\t' + p.right_centroid + '\n');
		f2.close();
	}
		
	// calculates the confidence of a tuple is: Conf(P_i) * DegreeMatch(P_i) 
	static void calculateTupleConfidence(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples) throws IOException {
		for (Tuple t : candidateTuples.keySet()) {									
			double confidence = 1;
			t.confidence_old = t.confidence;
			List<Pair<SnowballPattern, Double>> listPatterns = candidateTuples.get(t);
			for (Pair<SnowballPattern, Double> pair : listPatterns) {
				confidence *= ( 1 - (pair.getFirst().confidence() * pair.getSecond()) );
			}
			t.confidence = 1 - confidence;
			// if tuple was already seen use past confidence values to calculate new confidence
			// in the same fashion as for the patterns
			if (iter>0) {
				t.confidence = t.confidence * Config.parameters.get("wUpdt") + t.confidence_old * (1 - Config.parameters.get("wUpdt"));
			}
		}
	}
	
	// uses the extraction patterns to scan the sentences and find more tuples 
	static void generateTuples(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, LinkedList<SnowballPattern> patterns, String file) throws Exception {
		String sentence = null;
		Integer sentence_id = null;
		Integer url_id = null;
		String date = null;
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
		// find all possible pairs of seed e1_type and e2_type
		while ( ( sentence = f1.readLine() ) != null) {
			if (count % 10000 == 0) System.out.print(".");
			String[] parts = sentence.split("\t");
			if (parts.length<4) continue;
			url_id = Integer.parseInt(parts[0]);
			sentence_id = Integer.parseInt(parts[1]);
			date = parts[2];
			sentence = parts[3];
			Matcher matcher1 = pattern1.matcher(sentence);
			Matcher matcher2 = pattern2.matcher(sentence);			
			//make sure e2 is not the same as e1, if e1 and e2 have the same type
			//just run matcher2.find() to match the next occurrence
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
					
					// ignore contexts where another entity occur between the two entities
					String middleText = sentence.substring(matcher1.end(),matcher2.start());
            		Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
            		Matcher matcher = ptr.matcher(middleText);            		
	            	if (matcher.find()) continue;
	            	
					// constructs vectors considering only tokens outside tags               		
	            	String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	String right_txt = sentence.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            	
	            	left_t = TermsVector.normalize(left_txt);				
	        		middle_t = TermsVector.normalize(middle_txt);
					right_t = TermsVector.normalize(right_txt);
	                	
	                if (middle_t.size()<=Config.parameters.get("max_tokens_away") && middle_t.size()>=Config.parameters.get("min_tokens_away") && middle_t.size()>0) {
	                	
	                	// create a tuple for an occurrence found
	        			Tuple t = new Tuple(left_t, middle_t, right_t, e1, e2, sentence, date, url_id, sentence_id);        				
	        			double simBest = 0;
	        			SnowballPattern patternBest = null;
	        			List<Integer> patternsMatched = new LinkedList<Integer>();
	        			
	        			// calculate its similarity with each generated pattern
	        			for (SnowballPattern pattern : patterns) {
	        				
	        				Double similarity = null;
	        				
	        				if (Config.useWord2Vec==true)
	        					similarity = t.degreeMatchWord2Vec(pattern.w2v_left_sum_centroid,pattern.w2v_middle_sum_centroid,pattern.w2v_right_sum_centroid);
	        						        				
	        				else 
	        					similarity = t.degreeMatchCosTFIDF(pattern.left_centroid, pattern.middle_centroid, pattern.right_centroid);
	        				
	        				// if the similarity between the sentence where the tuple was extracted and the pattern is greater than a threshold
	        				// update the pattern confidence and its RlogF measure
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
	        				// find maximum confidence value
	        				for (SnowballPattern p : patterns) {
								if (p.RlogF>maxRlogF) maxRlogF=p.RlogF;
							}	        				
	        				// normalize
	        				for (Integer integer : patternsMatched) {
	        					SnowballPattern p = patterns.get(integer);
	        					if (p.RlogF>0) p.RlogF = p.RlogF / maxRlogF;
	        					else p.RlogF = 0;
	        				}			
	        			}
	        			// use confidence values from past iterations to calculate pattern confidence: updateConfidencePattern()
						if (iter>0) {							
							for (Integer i : patternsMatched) {
								SnowballPattern p = patterns.get(i);
								p.updateConfidencePattern();
								p.confidence_old = p.confidence;
								p.RlogF_old = p.RlogF;
							}							
						}
						
						if (simBest>=Config.parameters.get("min_degree_match")) {
        					List<Pair<SnowballPattern, Double>> list = null;
        					
							// create an object holding the pattern and the similarity score to the tuple that it extracted
        					Pair<SnowballPattern,Double> p = new Pair<SnowballPattern, Double>(patternBest, simBest);

        					// check if the tuple was already extracted in a previous iteration        					
        					Tuple tupleInCandidatesMap = getTuple(t, candidateTuples.keySet());
        					
        					// if tuple was not already extracted
        					if ( tupleInCandidatesMap == null ) {
        						list = new LinkedList<Pair<SnowballPattern,Double>>();        						
        						//list.add(p);
        						tupleInCandidatesMap = t;
        					}
        					// tuple was already extracted
        					else {        						
        						list = candidateTuples.get(tupleInCandidatesMap);
        						if (!list.contains(p)) list.add(p);       						
        					}
        					candidateTuples.put(tupleInCandidatesMap, list);
        					
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
		
	// gathers sentences based on initial seeds 
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
		String date = null;
		Integer sentence_id = null;
		Integer url_id = null;
		Tuple t = null;
    	int n_lines = 0;
		BufferedReader f = new BufferedReader(new FileReader(new File(sentencesFile)));	   	    
	    while ( ( sentence = f.readLine() ) != null ) {
			if (n_lines % 10000 == 0) System.out.print(".");
			String[] parts = sentence.split("\t");
			try {
				url_id = Integer.parseInt(parts[0]);
				sentence_id = Integer.parseInt(parts[1]);
				date = parts[2];
				sentence = parts[3];
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
	               
	                	// ignore sentences with certain words
	                	/*
	                	String[] exceptions = {"frisou","afirmou","garantiu","referiu"};                	
	                	List<String> except = Arrays.asList(exceptions);
	                	boolean ignore=false;
	                	
	                	for (String term : terms) {
							if (except.contains(term)) {
								ignore=true;
								break;	
							}						
						}                	
	                	if (ignore) continue;
	                	*/
	            		
	            		
	            		// ignore contexts where another entity occur between the two entities	    					
	    				String middleText = sentence.substring(matcher1.end(),matcher2.start());
	                	Pattern ptr = Pattern.compile("<[^>]+>[^<]+</[^>]+>");            		
	                	Matcher matcher = ptr.matcher(middleText);            		
	    	            if (matcher.find()) continue;
	    	            	
	            		//constructs vectors considering only tokens outside tags                		
	            		String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		String right_txt = sentence.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
	            		
	            		// normalize text
	            		left = TermsVector.normalize(left_txt);				
	        			middle = TermsVector.normalize(middle_txt);
						right = TermsVector.normalize(right_txt);
	                	
	                	if (middle.size()<=Config.parameters.get("max_tokens_away") && middle.size()>=Config.parameters.get("min_tokens_away") && middle.size()>0) {
		                	// generate a tuple with TF-IDF vectors and Word2Vec vectors
	                		t = new Tuple(left, middle, right, seed.e1, seed.e2, sentence, date, url_id, sentence_id);
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