package tuples;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import vsm.TermsVector;
import vsm.Word2Vec;
import bin.Config;

public class Tuple extends TermsVector implements Comparable<Tuple> {
	
	public String id;
	public Map<String,Double> left;
	public Map<String,Double> middle;
	public Map<String,Double> right;
	
	public float[] left_sum;
	public float[] middle_sum;
	public float[] right_sum;
	
	public float[] left_centroid;
	public float[] middle_centroid;
	public float[] right_centroid;
	
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
	
	public Tuple(List<String> left, List<String> middle, List<String> right, String e1, String e2, String t_sentence, String t_date, Integer t_url_id, Integer t_sentence_id) {
		super();		
		this.e1 = e1;
		this.e2 = e2;
		this.confidence = 0;
		this.confidence_old = 0;
		this.sentence = t_sentence;
		this.date = t_date;
		this.sentence_id = t_sentence_id;
		this.url_id = t_url_id;
		try {		

			/* use tokens only inside window interval */
			/* caculate TF-IDF of each term */
			
			/*
			if (left!=null) this.left = Config.vsm.tfidf(chopLeft(left));			
			if (middle!=null) this.middle = Config.vsm.tfidf(middle);			
			if (right!=null) this.right = Config.vsm.tfidf(chopRight(right));
			*/
			
			/* create word2vec representations */
			left_sum = Word2Vec.createVecSum(left);
			middle_sum = Word2Vec.createVecSum(middle);
			right_sum = Word2Vec.createVecSum(right);
			
			left_centroid = Word2Vec.createVecCentroid(left);
			middle_centroid = Word2Vec.createVecCentroid(middle);
			right_centroid = Word2Vec.createVecCentroid(right);
			
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
	
	public double degreeMatchWord2Vec(float[] w2v_left_sum_centroid, float[] w2v_middle_sum_centroid, float[] w2v_right_sum_centroid){	
		double l_w = Config.parameters.get("weight_left_context");
		double m_w = Config.parameters.get("weight_middle_context");
		double r_w = Config.parameters.get("weight_right_context");
		double left_similarity;
		double middle_similarity;
		double right_similarity;
				
		left_similarity = cosSimilarity(convertFloatDouble(this.left_sum),convertFloatDouble(w2v_left_sum_centroid))*l_w;
		middle_similarity = cosSimilarity(convertFloatDouble(this.middle_sum),convertFloatDouble(w2v_middle_sum_centroid))*m_w;
		right_similarity = cosSimilarity(convertFloatDouble(this.right_sum),convertFloatDouble(w2v_right_sum_centroid))*r_w;
		
		
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
}