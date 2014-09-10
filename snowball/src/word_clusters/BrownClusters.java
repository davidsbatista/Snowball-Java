package word_clusters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BrownClusters {
	
	public static HashMap<String, String> term_cluster = new HashMap<String, String>();
	
	/*
	 * read Brown Clusters from a file
	 */
	public static void readClusters(String clusterFile) {
		BufferedReader f = null;
		try {
			f = new BufferedReader(new FileReader( new File(clusterFile)));
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
			System.exit(0);
		}		
		String line = null;
		String[] tokens;
		int index = 0;
		try {
			while ( ( line = f.readLine() ) != null) {
				index++;
				try {
					if (line.startsWith("#") || line.isEmpty()) continue;
					tokens = line.split("\t");
					// entries (term, cluster)
					term_cluster.put(tokens[1], tokens[0]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Wrong line(" + index + ") in clustering file: " + line);
				} catch (Exception e) {
					System.out.println("Error parsing: " + line);
					e.printStackTrace();
					System.exit(0);
				} 				
			}
			f.close();
			List<String> clusters = new ArrayList<String>(term_cluster.values());
			Collections.sort(clusters);
			String biggestCluster = clusters.get(clusters.size()-1);
			biggestCluster += "0";
			term_cluster.put("withoutCluster", biggestCluster);
			System.out.println("without cluster: " + biggestCluster);
		} catch (IOException e) {
			System.out.println("I/O error");
			e.printStackTrace();
		}
		System.out.println("terms-cluster size: " + term_cluster.size());
	}
	
	public static String getTermCluster(String term) {
		String cluster = term_cluster.get(term);
		if (cluster == null) cluster = term_cluster.get("withoutCluster");
		return cluster;
	}

}
