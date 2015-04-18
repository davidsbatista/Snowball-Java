package vsm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.Stopwords;

public abstract class TermsVector {
		
	public static List<String> normalize(String text) {
		
		text = text.replaceAll("<[^>]+>[^<]+</?[^>]+> ","");
		
		// remove numbers, comma, parenthesis
		List<String> terms = new LinkedList<String>();		
		Pattern ptr = Pattern.compile("[0-9]+?|\\(|\\)|,|-+|:|\\.");			
		for (String term : Arrays.asList(text.split("\\s+?"))) {
			if (!term.equalsIgnoreCase("")) {
				Matcher matcher = ptr.matcher(term);
				if (!matcher.matches()) terms.add(term);
			}			
		}

		// convert terms to lower case representation
	    ListIterator<String> iterator = terms.listIterator();
	    while (iterator.hasNext()) iterator.set(iterator.next().toLowerCase());
	    
	    // remove stop-words and return
	    return Stopwords.removeStopWords(terms);
	}

	
	public static double norm(double[] a){
		double norm = 0;
		for (int i = 0; i < a.length; i++) {
			norm += Math.pow(a[i], 2);
		}						
		return Math.sqrt(norm);
	}
	
	
	// For TF-IDF vectors
	public static double cosSimilarity(Map<String,Double> a, Map<String,Double> b){
		double normA = norm(a);
		double normB = norm(b); 		
		if (normA==0 || normB==0) return 0;		
		return (double) innerProduct(a,b) / (normA*normB);
	}
	
	// For TF-IDF vectors
	public static double innerProduct(Map<String,Double> v1, Map<String,Double> v2) {		
		Double sum = 0.0;
		Set<String> smaller;
		if (v1.keySet().size() <= v2.keySet().size()) smaller = v1.keySet();
		else smaller = v2.keySet();		
		for (String term : smaller) {
			if (v1.containsKey(term) && v2.containsKey(term)) sum = sum + (v1.get(term) * v2.get(term));
		}		
		return sum;
	} 
	
	// For TF-IDF vectors
	public static double norm(Map<String,Double> v){
		double norm = 0;
		for (String w : v.keySet()) 			
			norm += Math.pow(v.get(w), 2);
		return Math.sqrt(norm);
	}	
	
	public static Map<String,Double> normalizeVector(Map<String,Double> v) {
		/* calculate norm of vector v */		
		double norm = norm(v);
		
		/* divide all components by the norm*/
		for (String word : v.keySet()) {
			double t = v.get(word);
			t = t / norm;
			v.put(word, t);
		}
		return v;
	}
}