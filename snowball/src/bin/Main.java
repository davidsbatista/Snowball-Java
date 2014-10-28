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

import tuples.REDSTuple;
import tuples.Tuple;
import utils.Pair;
import clustering.REDSPattern;
import clustering.SnowballPattern;

public class Main {
	
	static int iter = 0;
	
	public static void main(String[] args) throws Exception {
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + " Mbytes");
		long startTime = System.nanoTime();

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
			
			Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples = new HashMap<Tuple, List<Pair<SnowballPattern,Double>>>();
			List<SnowballPattern> patterns = new ArrayList<SnowballPattern>();
			
			Snowball.start(sentencesFile,seedsFile,candidateTuples,patterns);
			
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
		
		else if (system.equalsIgnoreCase("REDS")) {
			REDSConfig.init(parameters, sentencesFile);
			REDSConfig.readSeeds(seedsFile);
			if (REDSConfig.e1_type==null || REDSConfig.e2_type==null) {
				System.out.println("No semantic types defined");
				System.exit(0);
			}
			
			/* start a bootstrapping extraction */
			Map<REDSTuple, List<Pair<REDSPattern, Double>>> candidateTuples = new HashMap<REDSTuple, List<Pair<REDSPattern,Double>>>();
			List<REDSPattern> patterns = new ArrayList<REDSPattern>();			
			REDS.start(sentencesFile,seedsFile,candidateTuples,patterns);
			
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
			ArrayList<REDSTuple> tuplesOrdered  = new ArrayList<REDSTuple>(candidateTuples.keySet());				
			Collections.sort(tuplesOrdered);
			Collections.reverse(tuplesOrdered);
			for (REDSTuple t : tuplesOrdered) {
				f1.write("tuple:" + t.e1 + '\t' + t.e2 + '\t' + t.confidence + "\n");
				f1.write(t.sentence + "\n\n");
			}
			f1.close();
			for (REDSPattern p : patterns) {
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
	static void outputToFiles(Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples, List<SnowballPattern> patterns) throws IOException {
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
		if (SnowballConfig.algorihtm.equalsIgnoreCase("Snowball_classic") || SnowballConfig.algorihtm.equalsIgnoreCase("Snowball_word2vec")) {
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
		else if (SnowballConfig.algorihtm.equalsIgnoreCase("REDS")) {
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
	}
}