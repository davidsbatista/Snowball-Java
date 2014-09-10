package word_clusters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Word2VecClusters {
	
	static HashMap<String, String> word_cluster = new HashMap<String, String>();					// word -> cluster
	static HashMap<String, HashSet<String>> cluster_word = new HashMap<String, HashSet<String>>();	// cluster -> words
	public static int num_clusters = -1; 
	
	/* For testing only */
	public static void main(String[] args) throws IOException{
		readClusters(args[0]);
		String w = "apoia";
		String cluster_id = word_cluster.get(w);
		System.out.println("Id for cluster of word \"" + w + "\" " + cluster_id);
		HashSet<String> words = cluster_word.get(cluster_id);
		System.out.println("Words in cluster of \"" + w + "\" with id " + cluster_id + " " + words.size());
	}
	
	public static String getClusterID(String word) {
		return word_cluster.get(word);
	}
	
	public static HashSet<String> getWord(int clusterID) {
		return cluster_word.get(clusterID);
	}

	public static void readClusters(String clusterFile) throws IOException{
		String line = null;
		String previous_cluster = "0";
		String cluster_id = "0";
		HashSet<String> words = new HashSet<String>();
		BufferedReader f = new BufferedReader(new FileReader(new File(clusterFile)));	   	    
	    while ( ( line = f.readLine() ) != null ) {
	    	String[] parts = line.split(" ");
	    	String word = parts[0].trim();
	    	cluster_id = parts[1];
	    	word_cluster.put(word, cluster_id);
	    	if (previous_cluster.equalsIgnoreCase(cluster_id)) words.add(word);
	    	else {
	    		cluster_word.put(previous_cluster, words);
	    		words = new HashSet<String>();
	    	}
	    	previous_cluster = cluster_id;
	    }
	    cluster_word.put(previous_cluster, words);
	    System.out.println(word_cluster.keySet().size() + " words loaded");
	    System.out.println(cluster_word.keySet().size() + " clusters loaded");
	    num_clusters = word_cluster.size();
	    f.close();
	}

}







