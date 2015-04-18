package bin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import tuples.SnowballTuple;
import utils.Pair;
import clustering.SnowballPattern;

public class Main {
	
	static int iter = 0;
	
	public static void main(String[] args) throws Exception {
		long maxBytes = Runtime.getRuntime().maxMemory();
		System.out.println("Max memory: " + maxBytes / 1024 / 1024 + " Mbytes");
		
		if (args.length==0) {
			System.out.println("java -jar bootstrapping.jar sentencesFile parameters.cfg seedsFile");
			System.out.println();
			System.exit(0);
		}
		
		/* Read configuration files, sentence files, initial seeds files */
		String sentencesFile = args[0];		
		String parameters = args[1];
		String seedsFile = args[2];
		
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
		f1.write("Input file: " + sentencesFile);
		f1.write("\n");
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
}