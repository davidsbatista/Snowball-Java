package vsm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import nlp.Stopwords;

import org.jblas.FloatMatrix;

import bin.Config;

public abstract class TermsVector {
		
	// Normalize a string of text: remove "odd" characters, tokenize, remove stopwords, lowercase*/
	public static List<String> normalize(String text) {

		List<String> terms = new LinkedList<String>();
		
		// Remove tags, numbers, constructs vectors considering only tokens outside tags		
		text  = text.replaceAll("<[^>]+>[^<]+</?[^>]+>"," ").replaceAll("[0-9]+?(,|\\.|/)?([0-9]+)?.?(º|ª|%)?", "");
		
		// Tokenize
		terms = (List<String>) Arrays.asList(text.split("\\s"));

		// Remove stop-words
		if (Config.REDS==false) {
			terms = Stopwords.removeStopWords(terms);
		}
		
		// Convert terms to lower case representation
	    ListIterator<String> iterator = terms.listIterator();
	    while (iterator.hasNext()) iterator.set(iterator.next().toLowerCase());
	    		
		return terms;		
	}
	
	// Cosine Similarity between FloatMatrix vectors
	public static double cosSimilarity(FloatMatrix a, FloatMatrix b){		 		
		if (a.norm2()==0 || b.norm2()==0) return 0;
		return (double) dotProdut(a,b) / (a.norm2() * b.norm2());
	}
	
	public static double dotProdut(FloatMatrix a, FloatMatrix b) {		
		double sum = 0;
		for (int i = 0; i < b.length; i++) {
			sum = sum + (a.get(i) * b.get(i));
		}	
		return sum;
	} 
	
	public static double norm(double[] a){
		double norm = 0;
		for (int i = 0; i < a.length; i++) {
			norm += Math.pow(a[i], 2);
		}						
		return Math.sqrt(norm);
	}
	
	
	// for TF-IDF vectors
	public static double cosSimilarity(Map<String,Double> a, Map<String,Double> b){
		double normA = norm(a);
		double normB = norm(b); 		
		if (normA==0 || normB==0) return 0;		
		return (double) innerProduct(a,b) / (normA*normB);
	}
	
	// for TF-IDF vectors
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
	
	// for TF-IDF vectors
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