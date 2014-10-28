package clustering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jblas.FloatMatrix;

import tuples.REDSTuple;
import tuples.Seed;
import utils.Pair;
import vsm.CreateWord2VecVectors;
import vsm.TermsVector;
import bin.REDSConfig;
import bin.SnowballConfig;

public class REDSPattern {
	
	public Set<REDSTuple> tuples;
	public static Set<List<String>> patterns = new HashSet<List<String>>();
	
	// Expanded pattern
	public boolean expanded = false;
	
	// Word2Vec representations
	public FloatMatrix w2v_left_centroid;
	public FloatMatrix w2v_middle_centroid;
	public FloatMatrix w2v_right_centroid;
	
	public FloatMatrix w2v_centroid = new FloatMatrix();
	
	public int positive = 0;
	public int negative = 0;	
	public double confidence = 0;
	public double confidence_old = 0;
	public double RlogF = 0;	
	public double RlogF_old = 0;
	
	// Create a new cluster with just one tuple, which will be the centroid
	public REDSPattern(REDSTuple tuple){ 
		tuples = new HashSet<REDSTuple>();	
		
		this.tuples.add(tuple);
		this.w2v_left_centroid = tuple.left_sum;
		this.w2v_middle_centroid = tuple.middle_sum;
		this.w2v_right_centroid = tuple.right_sum;

	}
		
	public REDSPattern() {
		super();
		tuples = new HashSet<REDSTuple>();
	}

	public void mergeUniquePatterns(){
		for (REDSTuple t : this.tuples) {
			/*
			System.out.println(t.sentence);
			for (ReVerbPattern rvb : t.ReVerbpatterns) {
				System.out.println(rvb.token_words);
				System.out.println(rvb.token_universal_pos_tags);
				System.out.println(rvb.token_ptb_pos_tags);
				System.out.println();
			}
			*/
			if (t.ReVerbpatterns.size()>0) {
				patterns.add(t.ReVerbpatterns.get(0).token_words);
			}				
		}
	}
	
	public FloatMatrix sum() {
		mergeUniquePatterns();
		FloatMatrix sum = FloatMatrix.zeros(SnowballConfig.word2Vec_dim);
		for (List<String> pattern : this.patterns) {
			FloatMatrix p = CreateWord2VecVectors.createVecSum(pattern);
			sum.addi(p);
		}
		return sum;
	}
	
	public FloatMatrix centroid() {
		mergeUniquePatterns();
		FloatMatrix centroid = FloatMatrix.zeros(SnowballConfig.word2Vec_dim);
		for (List<String> pattern : this.patterns) {
			FloatMatrix p = CreateWord2VecVectors.createVecCentroid(pattern);
			centroid.addi(p);
		}		
		centroid = centroid.divi((float) this.patterns.size());		
		return centroid;
	}
	
	public static Pair<Boolean, Double> all(FloatMatrix vector) {		
		
		int good = 0;
		int bad = 0;
		double score = 0;
		double max_similarity = 0;
		
		for (List<String> relationalWords : patterns) {
			if (REDSConfig.single_vector.equalsIgnoreCase("sum")) {
				FloatMatrix a = CreateWord2VecVectors.createVecSum(relationalWords);
				score = TermsVector.cosSimilarity(a, vector);
				if (score > max_similarity) {					
					max_similarity = score;
				}
			}			
			if (REDSConfig.single_vector.equalsIgnoreCase("centroid")) {
				FloatMatrix a = CreateWord2VecVectors.createVecCentroid(relationalWords);
				score = TermsVector.cosSimilarity(a, vector);
				if (score > max_similarity) {					
					max_similarity = score;
				}
			}			
			if (score>=REDSConfig.similarity_threshold) good++;
			else bad++;
		}
		if (good>=bad) {
			return new Pair<Boolean, Double>(true, max_similarity);
		}
		else {
			return new Pair<Boolean, Double>(false, 0.0);
		}
	}
	
	
	public void updateConfidencePattern(){
		if (SnowballConfig.use_RlogF) {
			confidence = this.RlogF * SnowballConfig.wUpdt + this.RlogF_old * (1 - SnowballConfig.wUpdt);
		}
		else {
			confidence = this.confidence * SnowballConfig.wUpdt + this.confidence_old * (1 - SnowballConfig.wUpdt);
		}		
	}

	public void updatePatternSelectivity(String e1, String e2) {
		for (Seed s : SnowballConfig.seedTuples) {
			if (s.e1.equals(e1.trim()) || s.e1.trim().equals(e1.trim())) {
				if (s.e2.equals(e2.trim()) || s.e2.trim().equals(e2.trim())) {
					positive++;
				}
				else negative++;
			}
		}
	}

	public double confidence(){
		double conf = 0;
		if ((this.positive + this.negative)>0) {
			conf = (double) this.positive / (double) (this.positive + this.negative);
			this.confidence = conf; 
		}
		return conf;
	}
	
	// the RlogF confidence of pattern P/
	public void ConfidencePatternRlogF() {		
		if (this.confidence>0) {
			this.RlogF = this.confidence*(1+(Math.log(this.positive)/(Math.log(2))));
		}
		else this.RlogF = 0;		
	}
	
	public void addTuple(REDSTuple t){
		tuples.add(t);
	}	
}
