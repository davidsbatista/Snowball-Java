package vsm;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VectorSpaceModel implements Serializable {
	
	private static final long serialVersionUID = -2330445814515681648L;

	public TObjectIntMap<String> term_document_frequency;	/* hash table, where key is a term 't' and value is the number of documents where it occurs */	
	int n_docs = 0;											/* number of documents in the collection */

	
	public VectorSpaceModel(String sentencesFile) throws IOException {		
		BufferedReader sentencesBuffer = new BufferedReader(new FileReader(new File(sentencesFile)));		
	    String sentence = null;
	    term_document_frequency = new TObjectIntHashMap<String>(100000);    
	    Integer count;
	    while ( ( sentence = sentencesBuffer.readLine() ) != null ) {
	    	if (n_docs % 10000 == 0) System.out.print(".");	    	
	    	try {
				sentence = sentence.trim();
				// Normalize sentence and removed repeated
				// even if a term occurs more than once in a document just count it once				
				Set<String> terms = new HashSet<String>(TermsVector.normalize(sentence));					
				for (String t : terms) {
					count = term_document_frequency.get(t);	    			
					if (count==null) term_document_frequency.put(t,1);	    		
					else term_document_frequency.put(t, count+1);
				}
				n_docs++;
			} catch (Exception e) {
				System.out.println("Error generating term-frequency");
				e.printStackTrace();
				System.exit(0);
			}
	    }
	    sentencesBuffer.close();			
		/* Dimensionality of the vector is the number of words in the vocabulary (i.e., the number of distinct words in the corpus) */		
		/* Sort the vocabulary to keep the same dimension(terms/words) vectors */
		List<String> list = new LinkedList<String>(term_document_frequency.keySet());
		Collections.sort(list);		
		BufferedWriter f_terms = new BufferedWriter(new FileWriter(new File("terms.txt")));		
		for (String term : list) 
			f_terms.write(term+'\t'+String.valueOf(term_document_frequency.get(term))+"\n");		
		f_terms.close();
	}

	
	/* Number of times that each term 't' occurs in document D */
	public TObjectIntMap<String> termsFrequency(List<String> context) {
		TObjectIntMap<String> tf = new TObjectIntHashMap<String>(200);
		Integer value;
		for (String w : context) {	
			 value = tf.get(w);
			if (value==null) tf.put(w, 1);
			else {
				value++;
				tf.put(w, value);
			}
		}					
	return tf;
	}
	
	/* calculate a document vector according to its terms weights (TF-IDF) */
	public Map<String,Double> tfidf(List<String> document) throws Exception {		
		Map<String,Double> vector = new HashMap<String,Double>();
		TObjectIntMap<String> doc_tf = termsFrequency(document);
		double tf_idf = 0;
		for (String term : document) {
			int tf = doc_tf.get(term);
			double idf = 0;
			try {
				idf = Math.log( (float) n_docs / (double) (term_document_frequency.get(term)) );				
			} catch (Exception e) {
					System.out.println("\nterm: " + term);
					System.out.println("tf: " + doc_tf.get(term));
					System.out.println("idf: " + term_document_frequency.get(term));
					e.printStackTrace();
			}						
			if (Double.isInfinite(idf)) {
				System.out.println("\nterm: " + term);
				System.out.println("tf: " + doc_tf.get(term));
				System.out.println("idf: " + term_document_frequency.get(term));
				throw new Exception("Result is Infinite");
			}
			else tf_idf = tf*idf;	
			vector.put(term,tf_idf);
		}
		return vector;
	}
}
