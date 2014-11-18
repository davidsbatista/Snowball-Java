package bin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nlp.Stopwords;

import org.jblas.FloatMatrix;

import tuples.BREDSTuple;
import tuples.SnowballTuple;
import utils.Pair;
import clustering.BREDSPattern;
import clustering.SnowballPattern;

public class Main {
	
	static int iter = 0;
	
	public static void main(String[] args) throws Exception {
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + " Mbytes");
		
		if (args.length==0) {
			System.out.println("java -jar bootstrapping.jar [Snowball|REDS] sentencesFile parameters.cfg seedsFile");
			System.out.println();
			System.exit(0);
		}
		
		/* Read configuration files, sentence files, initial seeds files */
		String system = args[0];
		String sentencesFile = args[1];		
		String parameters = args[2];
		String seedsFile = args[3];
				
		if (system.equalsIgnoreCase("Snowball")) {
			SnowballConfig.init(parameters, sentencesFile);
			SnowballConfig.readSeeds(seedsFile);
			if (SnowballConfig.e1_type==null || SnowballConfig.e2_type==null) {
				System.out.println("No semantic types defined");
				System.exit(0);
			}
			
			/* create data structures and nanoTime */
			Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples = new HashMap<SnowballTuple, List<Pair<SnowballPattern,Double>>>();
			List<SnowballPattern> patterns = new ArrayList<SnowballPattern>();			
			long startTime = System.nanoTime();
			
			/* start a bootstrapping extraction */
			Snowball.start(sentencesFile,seedsFile,candidateTuples,patterns);
			
			/* calculate time taken to process */			
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
			
			BufferedWriter f1 = new BufferedWriter(new FileWriter("tuples.txt"));
			BufferedWriter f2 = new BufferedWriter(new FileWriter("patterns.txt"));
			ArrayList<SnowballTuple> tuplesOrdered  = new ArrayList<SnowballTuple>(candidateTuples.keySet());							
			Collections.sort(tuplesOrdered);
			Collections.reverse(tuplesOrdered);			
			int count = 0; 
			f1.write("Configuration parameters \n");			
			f1.write("min_degree_match: " + String.valueOf(SnowballConfig.min_degree_match) + "\n");
			f1.write("min_instance_confidance: " + String.valueOf(SnowballConfig.min_tuple_confidence) + "\n");
			f1.write("min_pattern_support: " + String.valueOf(SnowballConfig.min_pattern_support) + "\n");
			f1.write("\n");
			f1.write("weight_left_context: " + String.valueOf(SnowballConfig.weight_left_context) + "\n");
			f1.write("weight_middle_context: " + String.valueOf(SnowballConfig.weight_middle_context) + "\n");
			f1.write("weight_right_context: " + String.valueOf(SnowballConfig.weight_right_context) + "\n");
			f1.write("\n");			
			f1.write("wUpdt: " + String.valueOf(SnowballConfig.wUpdt) + "\n");
			f1.write("number_iterations: " + String.valueOf(SnowballConfig.number_iterations) + "\n");
			f1.write("use_RlogF: " + String.valueOf(SnowballConfig.use_RlogF) + "\n");			
			f1.write("\n");			
			for (SnowballTuple t : tuplesOrdered) {
				f1.write(count + "\n");
				f1.write("tuple:" + t.e1 + '\t' + t.e2 + '\t' + t.confidence + "\n");
				f1.write("sentence:" + t.sentence + "\n");
				f1.write("left: ");
				for (String word : t.left.keySet()) f1.write(word+' ');
				f1.write("\nmiddle: ");
				for (String word : t.middle.keySet()) f1.write(word+' ');
				f1.write("\nright: ");
				for (String word : t.right.keySet()) f1.write(word+' ');
				f1.write("\n\n");
				count++;
			}
			f1.close();			
			for (SnowballPattern p : patterns) {
				f2.write("confidence	:" + p.confidence+'\n');
				f2.write("#tuples		:" + p.tuples.size()+'\n');
				for (SnowballTuple tuple : p.tuples) {
					f2.write("\nleft: ");
					for (String word : tuple.left.keySet()) f2.write(word+',');
					f2.write("\nmiddle: ");
					for (String word : tuple.middle.keySet()) f2.write(word+',');
					f2.write("\nright: ");
					for (String word : tuple.right.keySet()) f2.write(word+',');
					f2.write("\n");
				}
				f2.write("\n================================================\n");
			}
			f2.close();
		}
		
		
		
		else if (system.equalsIgnoreCase("REDS")) {
			BREDSConfig.init(parameters, sentencesFile);
			BREDSConfig.readSeeds(seedsFile);
			if (BREDSConfig.e1_type==null || BREDSConfig.e2_type==null) {
				System.out.println("No semantic types defined");
				System.exit(0);
			}
			
			/* create data structures and nanoTime */
			Map<BREDSTuple, List<Pair<BREDSPattern, Double>>> candidateTuples = new HashMap<BREDSTuple, List<Pair<BREDSPattern,Double>>>();
			List<BREDSPattern> patterns = new ArrayList<BREDSPattern>();
			long startTime = System.nanoTime();
			
			/* start a bootstrapping extraction */
			BREDS.start(sentencesFile,seedsFile,candidateTuples,patterns);
			
			/* calculate time taken to process */			
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
			
			/* save results disk */
			BufferedWriter f1 = new BufferedWriter(new FileWriter("tuples.txt"));
			BufferedWriter f2 = new BufferedWriter(new FileWriter("patterns.txt"));
			ArrayList<BREDSTuple> tuplesOrdered  = new ArrayList<BREDSTuple>(candidateTuples.keySet());				
			Collections.sort(tuplesOrdered);
			Collections.reverse(tuplesOrdered);
			int count = 0; 
			f1.write("Configuration parameters \n");						
			f1.write("single vector: " + BREDSConfig.single_vector + "\n");
			f1.write("similarity : " + BREDSConfig.similarity + "\n");
			f1.write("threshold_similarity : " + String.valueOf(BREDSConfig.threshold_similarity) + "\n");
			f1.write("instance_confidance : " + String.valueOf(BREDSConfig.instance_confidance) + "\n");
			f1.write("min_pattern_support : " + String.valueOf(BREDSConfig.min_pattern_support) + "\n");
			f1.write("\n");
			f1.write("expand_patterns : " + String.valueOf(BREDSConfig.expand_patterns) + "\n");
			f1.write("expansion : " + String.valueOf(BREDSConfig.expansion) + "\n");
			f1.write("top_k : " + String.valueOf(BREDSConfig.top_k) + "\n");
			f1.write("pattern_drift : " + String.valueOf(BREDSConfig.pattern_drift) + "\n");
			f1.write("word2vec_model : " + String.valueOf(BREDSConfig.Word2VecModelPath) + "\n");
			f1.write("\n");
			f1.write("wUpdt : " + String.valueOf(BREDSConfig.wUpdt) + "\n");
			f1.write("number_iterations : " + String.valueOf(BREDSConfig.number_iterations) + "\n");
			f1.write("use_RlogF : " + String.valueOf(BREDSConfig.use_RlogF) + "\n");
			f1.write("\n");			
			for (BREDSTuple t : tuplesOrdered) {
				f1.write(count + "\n");
				f1.write("tuple:" + t.e1 + '\t' + t.e2 + '\t' + t.confidence + "\n");
				f1.write("sentence:" + t.sentence + "\n");
				f1.write("pattern: ");
				for (String token : t.ReVerbpatterns.get(0).token_words) {
					f1.write(token + " ");
				}
				f1.write("\n");
				f1.write("PoS-tags: ");
				for (String pos : t.ReVerbpatterns.get(0).token_universal_pos_tags) {
					f1.write(pos + " ");
				}
				f1.write("\n");
				f1.write("Vector(words considered): ");
				for (String w : t.ReVerbpatterns.get(0).token_words) {
					if (!Stopwords.stopwords.contains(w)) {
						try {
							float[] vector = BREDSConfig.word2vec.getWordVector(w);
							if (vector == null) continue;
							f1.write(w + " ");	
						} catch (Exception e) {
						}
					}					
				}
				f1.write("\n\n");
				count++;				
			}
			f1.close();
			for (BREDSPattern p : patterns) {
				f2.write("positive		:" + p.positive+'\n');
				f2.write("negative		:" + p.negative+'\n');
				f2.write("confidence	:" + p.confidence+'\n');
				f2.write("#tuples		:" + p.tuples.size()+'\n');
				f2.write("patterns		:" + p.patterns+'\n');
				f2.write("\n================================================\n");				
			}
			f2.close();
		}
	}

		
	/*
	 * Writes the extracted Tuples and the generated Extraction Patterns to files
	 */ 
	static void outputToFiles(Map<SnowballTuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException {
		/*
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
		if (REDSConfig.algorihtm.equalsIgnoreCase("Snowball_classic") || REDSConfig.algorihtm.equalsIgnoreCase("Snowball_word2vec")) {
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
		else if (REDsConfig.algorihtm.equalsIgnoreCase("REDS")) {
			for (SnowballPattern p : patterns) {
				f2.write("positive		:" + p.positive+'\n');
				f2.write("negative		:" + p.negative+'\n');
				f2.write("confidence	:" + p.confidence+'\n');
				f2.write("#tuples		:" + p.tuples.size()+'\n');
				f2.write("patterns		:" + p.patterns+'\n');
				f2.write("\n================================================\n");				
			}				
		}			
		f2.close();
		*/		
	}
}