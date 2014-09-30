package vsm;

import java.util.LinkedList;
import java.util.List;

import nlp.Stopwords;

import org.jblas.FloatMatrix;

import bin.Config;

public class CreateWord2VecVectors {
	
	public static FloatMatrix createVecSum(List<String> text){		
		FloatMatrix sum = new FloatMatrix(Config.word2Vec_dim);
		List<String> words_vector = new LinkedList<String>();
		for (String w : text) {
			if (!Stopwords.stopwords.contains(w)) {
				words_vector.add(w);
				try {
					float[] vector = Config.word2vec.getWordVector(w);
					if (vector == null) continue;
					else {
						FloatMatrix v = new FloatMatrix(vector);
						sum.addi(v);
					}			
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e);
					System.out.println(w);
				}
			}
			/*
			else {
				System.out.println("discarded: " + w);
			}
			*/			
		}
		//TODO:log
		/*
		System.out.println("ReVerb pattern/Relational words:" + text);
		System.out.println("Words used for vector :" + words_vector);
		System.out.println();
		*/
		return sum;
	}
		
	public static FloatMatrix createVecCentroid(List<String> text){
		FloatMatrix centroid = new FloatMatrix(Config.word2Vec_dim);
		for (String word : text) {
			if (!Stopwords.stopwords.contains(text)) {
				try {
					float[] vector = Config.word2vec.getWordVector(word);
					if (vector == null) continue;
					else {
						FloatMatrix v = new FloatMatrix(vector);
						centroid.addi(v);
					}		 		
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(e);
					System.out.println(word);
				}
			}
		}
		centroid = centroid.divi((float) Config.word2Vec_dim);		
		return centroid;
	}
}
