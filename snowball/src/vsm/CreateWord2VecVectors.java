package vsm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import nlp.Stopwords;
import org.jblas.FloatMatrix;

import utils.Pair;
import bin.BREDSConfig;

public class CreateWord2VecVectors {
	
	public static Pair<FloatMatrix,List<String>> createVecSum(Collection<String> text) {
		FloatMatrix sum = new FloatMatrix(BREDSConfig.word2Vec_dim);
		List<String> words = new LinkedList<String>();
		for (String w : text) {
			if (!Stopwords.stopwords.contains(w)) {
				try {
					float[] vector = BREDSConfig.word2vec.getWordVector(w);
					if (vector == null) continue;
					else {
						words.add(w);
						FloatMatrix v = new FloatMatrix(vector);
						sum.addi(v);
					}			
				} catch (Exception e) {
					//TODO:log words not_found
					//System.out.println("w);
				}
			}		
		}
		Pair<FloatMatrix, List<String>> p = new Pair<FloatMatrix,List<String>>(sum,words); 
		return p;
	}
		
	public static Pair<FloatMatrix,List<String>> createVecCentroid(Collection<String> text){
		FloatMatrix centroid = new FloatMatrix(BREDSConfig.word2Vec_dim);
		List<String> words = new LinkedList<String>();
		int number_words = 0;
		for (String w : text) {
			if (!Stopwords.stopwords.contains(w)) {
				try {
					float[] vector = BREDSConfig.word2vec.getWordVector(w);
					if (vector == null) continue;
					else {
						words.add(w);
						FloatMatrix v = new FloatMatrix(vector);
						centroid.addi(v);
						number_words++;
					}		 		
				} catch (Exception e) {
					//TODO:log words not_found
					//System.out.println("word not found: " + w);
				}
			}
		}
		if (number_words>1) {
			centroid = centroid.divi((float) number_words);
		}
		Pair<FloatMatrix, List<String>> p = new Pair<FloatMatrix,List<String>>(centroid,words); 
		return p;
	}
}
