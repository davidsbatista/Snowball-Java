package tuples;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.PortuguesePoSTagger;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.jblas.FloatMatrix;

import vsm.TermsVector;
import vsm.Word2Vec;
import bin.Config;

public class Tuple extends TermsVector implements Comparable<Tuple>, Clusterable {
	
	public String id;
	public Map<String,Double> left;
	public Map<String,Double> middle;
	public Map<String,Double> right;
	
	public Set<String> left_words;
	public Set<String> middle_words;
	public Set<String> right_words;
	
	public String middle_text;
	
	public List<FloatMatrix> patternsWord2Vec;
	public List<String> patterns;	
	
	public FloatMatrix left_sum;
	public FloatMatrix middle_sum;
	public FloatMatrix right_sum;
	
	public FloatMatrix left_centroid;
	public FloatMatrix middle_centroid;
	public FloatMatrix right_centroid;
	
	public String e1;
	public String e2;
	public String date;
	public double confidence_old;
	public double confidence;
	public String sentence;
	public Integer url_id;
	public Integer sentence_id;
	
	public Tuple() {
		super();
	}
	
	public Tuple(List<String> left, List<String> middle, List<String> right, String e1, String e2, String t_sentence, String t_middle_txt) {
		super();		
		this.e1 = e1;
		this.e2 = e2;
		this.confidence = 0;
		this.confidence_old = 0;
		this.sentence = t_sentence;
		this.left_words = new HashSet<String>();
		this.middle_words = new HashSet<String>();
		this.right_words = new HashSet<String>();
		this.patternsWord2Vec = new LinkedList<FloatMatrix>();
		this.patterns = new LinkedList<String>();
		
		try {
			
			/* use tokens only inside window interval */

						
			/* create word2vec representations */
				
			// sum vectors
			left_sum = Word2Vec.createVecSum(chopLeft(left));
			middle_sum = Word2Vec.createVecSum(middle);
			right_sum = Word2Vec.createVecSum(chopRight(right));
			
			// centroid of vectors
			left_centroid = Word2Vec.createVecCentroid(chopLeft(left));
			middle_centroid = Word2Vec.createVecCentroid(middle);
			right_centroid = Word2Vec.createVecCentroid(chopRight(right));
			
			// keep words				
			left_words.addAll(left);
			middle_words.addAll(middle);
			right_words.addAll(right);				
			this.middle_text = t_middle_txt;

			
			/* Compute TF-IDF of each term */
			
			if (left!=null) this.left = Config.vsm.tfidf(chopLeft(left));			
			if (middle!=null) this.middle = Config.vsm.tfidf(middle);			
			if (right!=null) this.right = Config.vsm.tfidf(chopRight(right));				
			
			/* Extract ReVerb patterns and construct Word2Vec representations */
			
			if (Config.extract_ReVerb==true) {				
				List<String> patterns = PortuguesePoSTagger.extractRVBPatterns(t_middle_txt);				
				if (patterns.size()>0) {
					// Sum each word vector of each patterns
					for (String pattern : patterns) {
						this.patterns.add(pattern);
						String[] t = pattern.split("_");
						List<String> tokens = (List<String>) Arrays.asList(t);
						
						//TODO: if pattern contains only one verb and is an auxialiary verb
						// discard pattern
						// e.g.(Portuguese): "é_PRE", "foi_PRE", "ser_PRE"												
						
						
						FloatMatrix patternWord2Vec = Word2Vec.createVecSum(tokens);
						this.patternsWord2Vec.add(patternWord2Vec);
					}
				}
				else {
					// If no ReVerb patterns are found
					// add middle words as if it was a pattern
					// TODO: discard ADV and ADJ					
					this.patterns.add(middle_text);
					FloatMatrix patternWord2Vec = Word2Vec.createVecSum(middle);
					this.patternsWord2Vec.add(patternWord2Vec);					
				}
			}
			
		} catch (Exception e) {
			System.out.println(sentence);
			System.out.println("e1: " + e1);
			System.out.println("e2: " + e2);
			System.out.println("left:");
			System.out.println(left);
			System.out.println(this.left);
			System.out.println();			
			System.out.println("middle:");
			System.out.println(middle);
			System.out.println(this.middle);
			System.out.println();					
			System.out.println("right:");
			System.out.println(right);
			System.out.println(this.right);
			System.out.println();			
			System.out.println("***************************************");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static List<String> chopLeft(List<String> left){		
		List<String> choped_left_terms = new LinkedList<String>();		
		if ((left.size()>=Config.parameters.get("context_window_size"))) {
			//gather terms by the end of list
			for (int i = left.size()-1; i > left.size()-1-Config.parameters.get("context_window_size"); i--) choped_left_terms.add(left.get(i));
		} else choped_left_terms = left;		
		return choped_left_terms;		
	}
	
	public static List<String> chopRight(List<String> right){		
		List<String> choped_right_terms = new LinkedList<String>();		
		if (right.size()>=Config.parameters.get("context_window_size")) {
			for (int i = 0; i < Config.parameters.get("context_window_size"); i++) choped_right_terms.add(right.get(i));
		} else choped_right_terms = right;		
		return choped_right_terms;
	}
	
	public static List<String> chopMiddle(List<String> middle){		
		List<String> choped_middle_terms = new LinkedList<String>();
		if (middle.size() <= Config.parameters.get("context_window_size")) choped_middle_terms = middle;
		else {
			//e1 + #window tokens at right
			for (int i = 0; i <= Config.parameters.get("context_window_size"); i++) choped_middle_terms.add(middle.get(i));						
			//e2 + #window tokens at left
			List<String> tmp = new LinkedList<String>();
			for (int i = middle.size()-1; i >= middle.size()-1-Config.parameters.get("context_window_size"); i--) tmp.add(middle.get(i));						
			//reverse list
			Collections.reverse(tmp);
			choped_middle_terms.addAll(tmp);			
		}		
		return choped_middle_terms;
	}
	
	public double[] convertFloatDouble(float[] v) {
		double[] w = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			w[i] = (double) v[i];
		}
		return w;
	}
	
	public double degreeMatchWord2Vec(FloatMatrix w2v_left_sum_centroid, FloatMatrix w2v_middle_sum_centroid, FloatMatrix w2v_right_sum_centroid){	
		double l_w = Config.parameters.get("weight_left_context");
		double m_w = Config.parameters.get("weight_middle_context");
		double r_w = Config.parameters.get("weight_right_context");
		double left_similarity;
		double middle_similarity;
		double right_similarity;
				
		left_similarity = cosSimilarity(this.left_sum,w2v_left_sum_centroid)*l_w;
		middle_similarity = cosSimilarity(this.middle_sum,w2v_middle_sum_centroid)*m_w;
		right_similarity = cosSimilarity(this.right_sum,w2v_right_sum_centroid)*r_w;
		
		return 	(left_similarity + middle_similarity + right_similarity);
	}
	
	public double degreeMatchCosTFIDF(Map<String,Double> t_left_vector, Map<String,Double> t_middle_vector, Map<String,Double> t_right_vector){	
		double l_w = Config.parameters.get("weight_left_context");
		double m_w = Config.parameters.get("weight_middle_context");
		double r_w = Config.parameters.get("weight_right_context");
		double left_similarity;
		double middle_similarity;
		double right_similarity;
		
		//TODO: corrigir, apenas se ambos têm os contextos BEF,AFT vazios		
		if (t_left_vector.size()==0 && t_right_vector.size()==0) {			
			left_similarity = 0;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*(m_w+l_w+r_w);
			right_similarity = 0;
		}
		
		//TODO: corrigir, apenas se ambos têm o contextos BEF vazio
		else if (t_left_vector.size()==0) {
			left_similarity = 0;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*(m_w+l_w);
			right_similarity = cosSimilarity(this.right,t_right_vector)*r_w;
		}
		
		//TODO: corrigir, apenas se ambos têm o contexto AFT vazio
		else if (t_right_vector.size()==0) {
			left_similarity = cosSimilarity(this.left,t_left_vector)*l_w;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*(m_w+r_w);
			right_similarity = 0;
		}

		else {
			left_similarity = cosSimilarity(this.left,t_left_vector)*l_w;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*m_w;
			right_similarity = cosSimilarity(this.right,t_right_vector)*r_w;
		}
		
		return 	(left_similarity + middle_similarity + right_similarity);
	}
	
	public boolean equals(Object obj) {
  	  if (obj == null) return false;
  	  if (obj == this) return true;
  	  if (!(obj instanceof Tuple)) return false;
  	  Tuple otherTuple = (Tuple) obj;
  	  if (this.e1.equals(otherTuple.e1) && this.e2.equals(otherTuple.e2) && this.sentence.equals(otherTuple.sentence)) return true;
  	  else return false;
    }

	@Override
	public int compareTo(Tuple t) {
		int result = 0;
		if (this.confidence>t.confidence) {
			result = 1;
		}
		else if (this.confidence<t.confidence) {
			result = -1;
		}
		else if (this.confidence==t.confidence) result = 0;
		return result;
	}	

	public String toString(){
		return e1 + '\t' + e2;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.math3.ml.clustering.Clusterable#getPoint()
	 * 
	 * returns the n-dimensional vector to be used by DBSCAN
	 */
	public double[] getPoint() {
		FloatMatrix v = this.patternsWord2Vec.get(0);		
		float[] t = v.toArray();
		double[] vector = new double[t.length];
		for (int i = 0; i < t.length; i++) {
			vector[i] = t[i];
		}
		return vector;
	}
}


