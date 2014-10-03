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

import tuples.Tuple;
import utils.Pair;
import clustering.SnowballPattern;

public class Main {
	
	static int iter = 0;
	
	public static void main(String[] args) throws Exception {
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + " Mbytes");
		long startTime = System.nanoTime();

		if (args.length==0) {
			System.out.println("java -jar snowbal.jar sentencesFile parameters.cfg seedsFile word2vecmodelpath");
			System.out.println();
			System.exit(0);
		}
		
		/* Read configuration files, sentence files, initial seeds files */
		String sentencesFile = args[0];
		String parameters = args[1];
		String seedsFile = args[2];
		String word2vecmodelPath = args[3];
		Config.init(parameters, sentencesFile, word2vecmodelPath);
		Config.readSeeds(seedsFile);			
		if (Config.e1_type==null || Config.e2_type==null) {
			System.out.println("No semantic types defined");
		}
		Map<Tuple, List<Pair<SnowballPattern, Double>>> candidateTuples = new HashMap<Tuple, List<Pair<SnowballPattern,Double>>>();
		List<SnowballPattern> patterns = new ArrayList<SnowballPattern>();

		/*
		TestWord2Vec.main();
		System.exit(0);
		*/
		
		System.out.println();
		
		// Starts REDS extraction process
		if (Config.REDS==true) REDS.start(sentencesFile,seedsFile,candidateTuples,patterns);			
		
		// Starts Snowball extraction process		
		else if (Config.REDS==false) Snowball.start(sentencesFile,seedsFile,candidateTuples,patterns);			 
		
		// Calculates running time and writes Patterns and Tuple to file
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