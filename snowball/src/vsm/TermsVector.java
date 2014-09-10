package vsm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import nlp.Stopwords;
import bin.Config;

public abstract class TermsVector {
		
	// normalize a string of text: remove "odd" characters, tokenize, remove stopwords, lowercase*/
	public static List<String> normalize(String text) {

		List<String> terms = new LinkedList<String>();
		
		/* clean tags and numbers, constructs vectors considering only tokens outside tags */		
		text  = text.replaceAll("<[^>]+>[^<]+</?[^>]+>"," ").replaceAll("[0-9]+?(,|\\.|/)?([0-9]+)?.?(º|ª|%)?", "");
		
		/* tokenize  */		
		terms = (List<String>) Arrays.asList(Config.tokenizer.tokenize(text));
				
		/* normalize verbs
		List<String> new_terms = new LinkedList<String>();
		for (String word : terms) {
			String w = PortugueseVerbNormalizer.normalizeVerb(word);
			new_terms.add(w);
		}		
		terms = new_terms;
		*/
		
		/* remove ".", e.g : "afirmou." */
		for (int i = 0; i < terms.size(); i++) {
			if (terms.get(i).endsWith(".")) terms.set(i, terms.get(i).replace(".", ""));
		}
		
		/* remove stopwords */
		terms = Stopwords.removeStopWords(terms);
		
		/* lowercase everything */
	    ListIterator<String> iterator = terms.listIterator();
	    while (iterator.hasNext()) iterator.set(iterator.next().toLowerCase());
	    
	    /* Stemme words */
	    //iterator = terms.listIterator();
	    //while (iterator.hasNext()) iterator.set(StemmerWrapper.stem(iterator.next()));
		
		return terms;		
	}
	
	// for Word2Vec vectors
	public static double cosSimilarity(double[] a, double[] b){
		double normA = norm(a);
		double normB = norm(b); 		
		if (normA==0 || normB==0) return 0;		
		return (double) dotProdut(a,b) / (normA*normB);
	}
	
	public static double dotProdut(double[] a, double[] b) {		
		double sum = 0;
		for (int i = 0; i < b.length; i++) {
			sum = sum + (a[i] * b[i]);
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