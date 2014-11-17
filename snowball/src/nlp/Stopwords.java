package nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Stopwords {
	
	public static Set<String> stopwords = new HashSet<String>();
	
	public static void loadStopWords(String file) throws IOException {
		BufferedReader f = new BufferedReader(new FileReader(new File(file)));
	    String word = null;
	    while ( ( word = f.readLine() ) != null ) {
	    	if (word.startsWith("#")) continue;
	    	stopwords.add(word.trim());
	    }
	    f.close();
	}
	
	public static List<String> removeStopWords(List<String> words) {
		List<String> result = new LinkedList<String>();
		for (String w : words) {
			if (stopwords.contains(w.toLowerCase().trim())) continue;
			else result.add(w);
		}
		return result;
	}
}
