package vsm;

import java.util.Collection;
import nlp.Stopwords;
import org.jblas.FloatMatrix;
import bin.BREDSConfig;

public class CreateWord2VecVectors {
	
	public static FloatMatrix createVecSum(Collection<String> text) {
		FloatMatrix sum = new FloatMatrix(BREDSConfig.word2Vec_dim);
		for (String w : text) {
			if (!Stopwords.stopwords.contains(w)) {
				try {
					float[] vector = BREDSConfig.word2vec.getWordVector(w);
					if (vector == null) continue;
					else {
						FloatMatrix v = new FloatMatrix(vector);
						sum.addi(v);
					}			
				} catch (Exception e) {
					//TODO:log words not_found
					//System.out.println("w);
				}
			}		
		}
		return sum;
	}
		
	public static FloatMatrix createVecCentroid(Collection<String> text){
		FloatMatrix centroid = new FloatMatrix(BREDSConfig.word2Vec_dim);
		int number_words = 0;
		for (String w : text) {
			if (!Stopwords.stopwords.contains(w)) {
				try {
					float[] vector = BREDSConfig.word2vec.getWordVector(w);
					if (vector == null) continue;
					else {
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
		return centroid;
	}
}
