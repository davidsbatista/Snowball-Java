package tuples;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.EnglishPoSTagger;
import nlp.ReVerbPattern;

import org.apache.commons.lang3.StringUtils;
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
		this.middle_words = new HashSet<String>();
		this.middleReverbPatternsWord2VecSum = new LinkedList<FloatMatrix>();
		this.ReVerbpatterns = new LinkedList<ReVerbPattern>();
	}
	
	public Tuple(String left, String middle, String right, String e1, String e2, String t_sentence) {
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
			// Use the same approach as Snowball but use Word2Vec to represent sentence 
			if ( Config.algorihtm.equalsIgnoreCase("Snowball_Word2vec")) {
								
				// For each context sum the vectors of the words vectors representations
				left_sum = CreateWord2VecVectors.createVecSum(TermsVector.normalize(getLeftContext(left)));
				middle_sum = CreateWord2VecVectors.createVecSum(TermsVector.normalize(middle));
				right_sum = CreateWord2VecVectors.createVecSum(TermsVector.normalize(getRightContext(right)));
				
				// For each context calculate the centroid of the words vectors representations
				left_centroid = CreateWord2VecVectors.createVecCentroid(TermsVector.normalize(getLeftContext(left)));
				middle_centroid = CreateWord2VecVectors.createVecCentroid(TermsVector.normalize(middle));
				right_centroid = CreateWord2VecVectors.createVecCentroid(TermsVector.normalize(getRightContext(right)));
				
				// Save words
				left_words.addAll(TermsVector.normalize(getLeftContext(left)));
				middle_words.addAll(TermsVector.normalize(middle) );
				right_words.addAll(TermsVector.normalize(getRightContext(right)));
				
				/*
				System.out.println(sentence);
				System.out.println(left_words);
				System.out.println(middle_words);
				System.out.println(right_words);
				System.out.println();
				*/
				
			}			
			
			// Create TF-IDF representations
			if ( Config.algorihtm.equalsIgnoreCase("Snowball_Classic")) {				
				if (left!=null) this.left = Config.vsm.tfidf(TermsVector.normalize(getLeftContext(left)));				
				if (middle!=null) this.middle = Config.vsm.tfidf(TermsVector.normalize(middle));
				if (right!=null) this.right = Config.vsm.tfidf(TermsVector.normalize(getRightContext(right)));
				
				// Save words
				left_words.addAll(TermsVector.normalize(getLeftContext(left)));
				middle_words.addAll(TermsVector.normalize(middle) );
				right_words.addAll(TermsVector.normalize(getRightContext(right)));				
			}
			
			// Extract ReVerb patterns and construct Word2Vec representations 			
			if ( Config.algorihtm.equalsIgnoreCase("REDS")) {
				List<ReVerbPattern> patterns = EnglishPoSTagger.extractRVB(middle);
				boolean discard = false;				
				if (patterns.size()>0) {
					
					//TODO: using only the first pattern, are there really more than 1 pattern in a middle context ?					
					List<String> pattern_tokens = patterns.get(0).token_words;
					List<String> pattern_ptb_pos = patterns.get(0).token_ptb_pos_tags;
					List<String> pattern_universal_pos = patterns.get(0).token_universal_pos_tags;		
					
					// If contains only an auxiliary VERB + IN discard
					// e.g.: is in, was out
					if (pattern_tokens.size()==2) {
						String verb = Config.EnglishLemm.lemmatize(pattern_tokens.get(0));
						if (Config.aux_verbs.contains(verb) && pattern_universal_pos.get(1).equalsIgnoreCase("ADP")) {
							discard = true;
						}
					}					
					if (!discard) {
						hasReVerbPatterns = true;
						this.ReVerbpatterns = patterns;						
						// Sum each word vector
						FloatMatrix patternWord2Vec = CreateWord2VecVectors.createVecSum(pattern_tokens);
						this.middleReverbPatternsWord2VecSum.add(patternWord2Vec);
					}
					
					//TODO: log discarded ReVerb patterns
					/*
					else {
						System.out.println("discarded:");
						System.out.println(pattern_tokens);
						System.out.println(pattern_ptb_pos);
						System.out.println(pattern_universal_pos);
						System.out.println();
					}
					*/					
				}
				
				// If no ReVerb patterns are found
				// add middle words and associated PoS-tags 
				// stored in ReVerbPattern object
				
				else if (patterns.size()==0 || discard) {
					hasReVerbPatterns = false;
					ReVerbpatterns = EnglishPoSTagger.tagSentence(middle);
					FloatMatrix patternWord2Vec = CreateWord2VecVectors.createVecSum(ReVerbpatterns.get(0).token_words);
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
	
	public static String getLeftContext(String left){		
		String[] left_tokens = left.split("\\s");
		List<String> tokens = new LinkedList<String>();
		if (left_tokens.length>=Config.context_window_size) {
			for (int i = left_tokens.length-1; i > left_tokens.length-1-Config.context_window_size; i--) tokens.add(left_tokens[i]);
		} else return left;
		String left_context = StringUtils.join(tokens," ");
		return left_context;
		
	}
	
	public static String getRightContext(String right){
		String[] right_tokens = right.split("\\s");
		List<String> tokens = new LinkedList<String>();		
		if (right_tokens.length>=Config.context_window_size) {
			for (int i = 0; i < Config.context_window_size; i++) {
				tokens.add(right_tokens[i]);
			}
		} else return right;
		String right_context = StringUtils.join(tokens," ");
		return right_context;
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
		
		/*
		System.out.println("left_similarity 	:" + cosSimilarity(this.left_sum,w2v_left_centroid));
		System.out.println("middle_similarity 	:" + cosSimilarity(this.middle_sum,w2v_middle_centroid));
		System.out.println("right_similarity 	:" + cosSimilarity(this.right_sum,w2v_right_centroid));
		System.out.println("similarity 			:" + String.valueOf(left_similarity + middle_similarity + right_similarity));
		System.out.println();
		*/
		
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
				
		if (t_left_vector.size()==0 && t_right_vector.size()==0) {			
			left_similarity = 0;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*(m_w+l_w+r_w);
			right_similarity = 0;
		}
				
		else if (t_left_vector.size()==0) {
			left_similarity = 0;
			middle_similarity = cosSimilarity(this.middle,t_middle_vector)*(m_w+l_w);
			right_similarity = cosSimilarity(this.right,t_right_vector)*r_w;
		}
				
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