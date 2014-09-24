package tuples;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.EnglishPoSTagger;
import nlp.ReVerbPattern;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.jblas.FloatMatrix;

import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import bin.Config;

public class Tuple extends TermsVector implements Comparable<Tuple>, Clusterable, Serializable {
	
	private static final long serialVersionUID = -6291870921472158824L;
	
	/* TF-IDF vectors */
	public Map<String,Double> left;
	public Map<String,Double> middle;
	public Map<String,Double> right;
	
	/* Bag-of-words */
	public Set<String> left_words;
	public Set<String> middle_words;
	public Set<String> right_words;
	
	public String middle_text;
	public List<ReVerbPattern> tagged_middle_text;
	
	public List<FloatMatrix> middleReverbPatternsWord2VecSum;
	public List<FloatMatrix> middleReverbPatternsWord2VecCentroid;
	
	/* ReVerb Patterns extracted from sentences */
	public List<ReVerbPattern> ReVerbpatterns;
	public boolean hasReVerbPatterns;
	
	public FloatMatrix left_sum;
	public FloatMatrix middle_sum;
	public FloatMatrix right_sum;
	
	public FloatMatrix left_centroid;
	public FloatMatrix middle_centroid;
	public FloatMatrix right_centroid;
	
	public String e1;
	public String e2;
	public double confidence_old;
	public double confidence;
	public String sentence;
	
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
		this.middleReverbPatternsWord2VecSum = new LinkedList<FloatMatrix>();
		this.ReVerbpatterns = new LinkedList<ReVerbPattern>();
		
		try {
			
			// keep words
			left_words.addAll(chopLeft(left));
			middle_words.addAll(middle);
			right_words.addAll(chopLeft(left));				
			this.middle_text = t_middle_txt;
			
			/* 
			 * Create Word2vec representations 
			 */				
			/*
			if (Config.useWord2Vec==true) {
				
				// for each context sum the words vectors representations
				left_sum = CreateWord2VecVectors.createVecSum(chopLeft(left));
				middle_sum = CreateWord2VecVectors.createVecSum(middle);
				right_sum = CreateWord2VecVectors.createVecSum(chopRight(right));
				
				// for each context calculate the centroid of the words vectors representations
				left_centroid = CreateWord2VecVectors.createVecCentroid(chopLeft(left));
				middle_centroid = CreateWord2VecVectors.createVecCentroid(middle);
				right_centroid = CreateWord2VecVectors.createVecCentroid(chopRight(right));				
			}
			*/
			if (Config.REDS==false) {			
			/* 
			 * Create TF-IDF representations 
			 */
				if (left!=null) this.left = Config.vsm.tfidf(chopLeft(left));			
				if (middle!=null) this.middle = Config.vsm.tfidf(middle);			
				if (right!=null) this.right = Config.vsm.tfidf(chopRight(right));				
			}
			
			/* 
			 * Extract ReVerb patterns and construct Word2Vec representations 
			 */			
			if (Config.REDS==true) {								
				this.ReVerbpatterns = EnglishPoSTagger.extractRVBPatterns(t_middle_txt);				
				if (this.ReVerbpatterns.size()>0) {
					hasReVerbPatterns = true;					
					//TODO: using only the first pattern, are there really more than 1 pattern in a middle context ?
					List<String> patterns_tokens = ReVerbpatterns.get(0).token_words;
					List<String> patterns_ptb_pos = ReVerbpatterns.get(0).token_ptb_pos_tags;
					List<String> patterns_universal_pos = ReVerbpatterns.get(0).token_universal_pos_tags;					
					// Sum each word vector
					//TODO: if pattern contains only one verb and is an auxiliary verb						
					FloatMatrix patternWord2Vec = CreateWord2VecVectors.createVecSum(patterns_tokens);
					this.middleReverbPatternsWord2VecSum.add(patternWord2Vec);
				}
				
				else {
					hasReVerbPatterns = false;
					// If no ReVerb patterns are found
					// add middle words as if it was a pattern
					// TODO: discard ADV and ADJ		
					tagged_middle_text = EnglishPoSTagger.tagSentence(middle_text);					
					/*
					System.out.println(this.sentence);
					String[] tokens = tagged.getFirst();
					String[] tags = tagged.getSecond();
					for (int i = 0; i < tagged.getFirst().length; i++) {
						System.out.println(tokens[i] + ' ' + tags[i]);
					}
					*/
					List<String> patterns_tokens = tagged_middle_text.get(0).token_words;
					
					this.ReVerbpatterns = tagged_middle_text;
					FloatMatrix patternWord2Vec = CreateWord2VecVectors.createVecSum(patterns_tokens);
					this.middleReverbPatternsWord2VecSum.add(patternWord2Vec);					
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
		if (left.size()>=Config.context_window_size) {
			//gather terms by the end of list
			for (int i = left.size()-1; i > left.size()-1-Config.context_window_size; i--) choped_left_terms.add(left.get(i));
		} else choped_left_terms = left;		
		return choped_left_terms;		
	}
	
	public static List<String> chopRight(List<String> right){		
		List<String> choped_right_terms = new LinkedList<String>();		
		if (right.size()>=Config.context_window_size) {
			for (int i = 0; i < Config.context_window_size; i++) choped_right_terms.add(right.get(i));
		} else choped_right_terms = right;		
		return choped_right_terms;
	}
	
	public static List<String> chopMiddle(List<String> middle){		
		List<String> choped_middle_terms = new LinkedList<String>();
		if (middle.size() <= Config.context_window_size) choped_middle_terms = middle;
		else {
			//e1 + #window tokens at right
			for (int i = 0; i <= Config.context_window_size; i++) choped_middle_terms.add(middle.get(i));						
			//e2 + #window tokens at left
			List<String> tmp = new LinkedList<String>();
			for (int i = middle.size()-1; i >= middle.size()-1-Config.context_window_size; i--) tmp.add(middle.get(i));						
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
	
	/* Word2Vec
	 * Computes the similarity using the sum of the words from each context  
	 */	
	public double degreeMatchWord2VecSum(FloatMatrix w2v_left_centroid, FloatMatrix w2v_middle_centroid, FloatMatrix w2v_right_centroid){	
		double l_w = Config.weight_left_context;
		double m_w = Config.weight_middle_context;
		double r_w = Config.weight_right_context;
		double left_similarity;
		double middle_similarity;
		double right_similarity;
				
		left_similarity = cosSimilarity(this.left_sum,w2v_left_centroid)*l_w;
		middle_similarity = cosSimilarity(this.middle_sum,w2v_middle_centroid)*m_w;
		right_similarity = cosSimilarity(this.right_sum,w2v_right_centroid)*r_w;
		
		return 	(left_similarity + middle_similarity + right_similarity);
	}
	
	/* Word2Vec
	 * Computes the similarity using the centroid of the words from each context  
	 */
	public double degreeMatchWord2VecCentroid(FloatMatrix w2v_left_centroid, FloatMatrix w2v_middle_centroid, FloatMatrix w2v_right_centroid){	
		double l_w = Config.weight_left_context;
		double m_w = Config.weight_middle_context;
		double r_w = Config.weight_right_context;
		double left_similarity;
		double middle_similarity;
		double right_similarity;
				
		left_similarity = cosSimilarity(this.left_centroid,w2v_left_centroid)*l_w;
		middle_similarity = cosSimilarity(this.middle_centroid,w2v_middle_centroid)*m_w;
		right_similarity = cosSimilarity(this.right_centroid,w2v_right_centroid)*r_w;
		
		return 	(left_similarity + middle_similarity + right_similarity);
	}
		
	public double degreeMatchCosTFIDF(Map<String,Double> t_left_vector, Map<String,Double> t_middle_vector, Map<String,Double> t_right_vector){	
		double l_w = Config.weight_left_context;
		double m_w = Config.weight_middle_context;
		double r_w = Config.weight_right_context;
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
	
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.math3.ml.clustering.Clusterable#getPoint()
	 * 
	 * returns the n-dimensional vector to be used by DBSCAN
	 */
	
	@Override
	public double[] getPoint() {
		if (Config.useReverb==true) {
			return getPointReVerb();
		}
		else return getPointMiddleSum();
	}

	public double[] getPointReVerb() {
		// Returns ReVerb patterns
		FloatMatrix v = this.middleReverbPatternsWord2VecSum.get(0);		
		float[] t = v.toArray();
		double[] vector = new double[t.length];
		for (int i = 0; i < t.length; i++) {
			vector[i] = t[i];
		}
		return vector;
	}
	
	public double[] getPointMiddleSum() {
		// Returns middle words summed
		FloatMatrix v = this.middle_sum;		
		float[] t = v.toArray();
		double[] vector = new double[t.length];
		for (int i = 0; i < t.length; i++) {
			vector[i] = t[i];
		}
		return vector;
	}
}