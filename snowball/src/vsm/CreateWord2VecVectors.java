package vsm;

import java.util.List;

import org.jblas.FloatMatrix;

import bin.Config;

public class CreateWord2VecVectors {
	
	public static FloatMatrix createVecSum(List<String> text){		
		FloatMatrix sum = new FloatMatrix(Config.word2Vec_dim);
		for (String word : text) {
			try {
				float[] vector = Config.word2vec.getWordVector(word);
				if (vector == null) continue;
				else {
					FloatMatrix v = new FloatMatrix(vector);
					sum.addi(v);
				}			
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e);
				System.out.println(word);
			}
		}
		return sum;
	}
		
	public static FloatMatrix createVecCentroid(List<String> text){
		FloatMatrix centroid = new FloatMatrix(Config.word2Vec_dim);
		for (String word : text) {
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
		centroid = centroid.divi((float) Config.word2Vec_dim);		
		return centroid;
	}
}
