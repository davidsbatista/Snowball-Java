package tuples;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import vsm.TermsVector;
import bin.SnowballConfig;

public class SnowballTuple extends TermsVector implements Comparable<SnowballTuple>, Serializable {
	
	private static final long serialVersionUID = -6291870921472158824L;
	
	/* TF-IDF vectors */
	public Map<String,Double> left;
	public Map<String,Double> middle;
	public Map<String,Double> right;
	
	public String e1;
	public String e2;
	public double confidence_old;
	public double confidence;
	public String sentence;
	
	public SnowballTuple() {
		super();
	}
	
	public SnowballTuple(List<String> left_,  List<String> middle_,  List<String> right_, String e1, String e2, String t_sentence) {
		super();		
		this.e1 = e1;
		this.e2 = e2;
		this.confidence = 0;
		this.confidence_old = 0;
		this.sentence = t_sentence;
		
		try {			
			// Create TF-IDF representations
			if (left_!=null)  
				this.left = SnowballConfig.vsm.tfidf(left_);
			if (middle_!=null); 
				this.middle = SnowballConfig.vsm.tfidf(middle_);
			if (right_!=null);
				this.right = SnowballConfig.vsm.tfidf(right_);
			
		} catch (Exception e) {
			System.out.println('\n'+sentence);
			System.out.println("left:");
			System.out.println(left_);
			System.out.println();			
			System.out.println("middle:");
			System.out.println(middle_);
			System.out.println(middle_.size());
			System.out.println(middle_.get(0));
			System.out.println(middle_.get(0).length());
			System.out.println(middle_.isEmpty());
			System.out.println();					
			System.out.println("right:");
			System.out.println(right_);
			System.out.println();			
			System.out.println("***************************************");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public double[] convertFloatDouble(float[] v) {
		double[] w = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			w[i] = (double) v[i];
		}
		return w;
	}
	
	public double degreeMatchCosTFIDF(Map<String,Double> t_left_vector, Map<String,Double> t_middle_vector, Map<String,Double> t_right_vector){	
		double l_w = SnowballConfig.weight_left_context;
		double m_w = SnowballConfig.weight_middle_context;
		double r_w = SnowballConfig.weight_right_context;
		double left_similarity = 0;
		double middle_similarity = 0;
		double right_similarity = 0;
			
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
  	  if (!(obj instanceof SnowballTuple)) return false;
  	  SnowballTuple otherTuple = (SnowballTuple) obj;
  	  if (this.e1.equals(otherTuple.e1) && this.e2.equals(otherTuple.e2) && this.sentence.equals(otherTuple.sentence)) return true;
  	  else return false;
    }

	@Override
	public int compareTo(SnowballTuple t) {
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