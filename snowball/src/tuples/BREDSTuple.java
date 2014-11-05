package tuples;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nlp.EnglishPoSTagger;
import nlp.ReVerbPattern;

import org.apache.commons.lang3.StringUtils;
import org.jblas.FloatMatrix;

import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import bin.BREDSConfig;
import bin.SnowballConfig;

public class BREDSTuple extends TermsVector implements Comparable<BREDSTuple>, Serializable {
	
	private static final long serialVersionUID = -6291870921472158824L;
	
	/* Bag-of-words */
	public Set<String> left_words;
	public Set<String> middle_words;
	public Set<String> right_words;
		
	public List<FloatMatrix> relationalWordsVector;
	
	/* ReVerb Patterns extracted from sentences */
	public List<ReVerbPattern> ReVerbpatterns;
	public boolean hasReVerbPatterns;
	
	public String e1;
	public String e2;
	public double confidence_old;
	public double confidence;
	public String sentence;

	public FloatMatrix left_sum;
	public FloatMatrix middle_sum;
	public FloatMatrix right_sum;
	
	public FloatMatrix middleReverbPatternsWord2VecSum;
	
	public BREDSTuple() {
		super();
		this.middle_words = new HashSet<String>();
		this.relationalWordsVector = new LinkedList<FloatMatrix>();
		this.ReVerbpatterns = new LinkedList<ReVerbPattern>();
	}
	
	public BREDSTuple(String left, String middle, String right, String e1, String e2, String t_sentence) {
		super();		
		this.e1 = e1;
		this.e2 = e2;
		this.confidence = 0;
		this.confidence_old = 0;
		this.sentence = t_sentence;
		this.left_words = new HashSet<String>();
		this.middle_words = new HashSet<String>();
		this.right_words = new HashSet<String>();
		this.relationalWordsVector = new LinkedList<FloatMatrix>();
		this.ReVerbpatterns = new LinkedList<ReVerbPattern>();
				
		// Save words
		left_words.addAll(TermsVector.normalize(getLeftContext(left)));
		middle_words.addAll(TermsVector.normalize(middle) );
		right_words.addAll(TermsVector.normalize(getRightContext(right)));
			
		// Extract ReVerb patterns and construct Word2Vec representations
		List<ReVerbPattern> patterns = EnglishPoSTagger.extractRVB(middle);
		boolean discard = false;				
		if (patterns.size()>0) {
					
			//TODO: using only the first pattern, are there really more than 1 pattern in a middle context ?					
			List<String> pattern_tokens = patterns.get(0).token_words;
			List<String> pattern_ptb_pos = patterns.get(0).token_ptb_pos_tags;
			List<String> pattern_universal_pos = patterns.get(0).token_universal_pos_tags;		
			
			/*
			<PER>Guterres</PER> was a founding member of the <ORG>Portuguese Refugee Council</ORG>
			extrai so: "was a" ?
			*/
			
			// If contains only an auxiliary VERB discard
			// e.g.: is in, was out, is a
			if (pattern_tokens.size()==2) {
				String verb = BREDSConfig.EnglishLemm.lemmatize(pattern_tokens.get(0));
				//if (BREADSConfig.aux_verbs.contains(verb) && pattern_universal_pos.get(1).equalsIgnoreCase("ADP")) {
				if (BREDSConfig.aux_verbs.contains(verb)) {
					discard = true;
				}
			}
			
			if (!discard) {
				hasReVerbPatterns = true;
				this.ReVerbpatterns = patterns;
				FloatMatrix patternWord2Vec = null;
				
				/*
				System.out.println(sentence);
				System.out.println(patterns.get(0).token_words);
				System.out.println(patterns.get(0).token_universal_pos_tags);
				System.out.println();
				*/				
				
				// Sum each word vector				
				if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) {
					patternWord2Vec = CreateWord2VecVectors.createVecSum(pattern_tokens);
				}
				// Centroid of each word vector
				else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) {
					patternWord2Vec = CreateWord2VecVectors.createVecCentroid(pattern_tokens);
				}				
				this.relationalWordsVector.add(patternWord2Vec);
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
				
		// If no ReVerb patterns are found, extract relational words, words between entities 
		// stored in ReVerbPattern object
	
		else if (patterns.size()==0 || discard) {
			hasReVerbPatterns = false;
			ReVerbpatterns = EnglishPoSTagger.tagSentence(middle);
			FloatMatrix patternWord2Vec = null;
			if (BREDSConfig.single_vector.equalsIgnoreCase("sum")) {
				patternWord2Vec = CreateWord2VecVectors.createVecSum(ReVerbpatterns.get(0).token_words);
			}
			else if (BREDSConfig.single_vector.equalsIgnoreCase("centroid")) {
				patternWord2Vec = CreateWord2VecVectors.createVecCentroid(ReVerbpatterns.get(0).token_words);
			}
			this.relationalWordsVector.add(patternWord2Vec);
		}
	}

	
	public static String getLeftContext(String left){		
		String[] left_tokens = left.split("\\s");
		List<String> tokens = new LinkedList<String>();
		if (left_tokens.length>=BREDSConfig.context_window_size) {
			for (int i = left_tokens.length-1; i > left_tokens.length-1-SnowballConfig.context_window_size; i--) tokens.add(left_tokens[i]);
		} else return left;
		String left_context = StringUtils.join(tokens," ");
		return left_context;
		
	}
	
	public static String getRightContext(String right){
		String[] right_tokens = right.split("\\s");
		List<String> tokens = new LinkedList<String>();		
		if (right_tokens.length>=BREDSConfig.context_window_size) {
			for (int i = 0; i < BREDSConfig.context_window_size; i++) {
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
	
	public boolean equals(Object obj) {
  	  if (obj == null) return false;
  	  if (obj == this) return true;
  	  if (!(obj instanceof BREDSTuple)) return false;
  	  BREDSTuple otherTuple = (BREDSTuple) obj;
  	  if (this.e1.equals(otherTuple.e1) && this.e2.equals(otherTuple.e2) && this.sentence.equals(otherTuple.sentence)) return true;
  	  else return false;
    }

	@Override
	public int compareTo(BREDSTuple t) {
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
}