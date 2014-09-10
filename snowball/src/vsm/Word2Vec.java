package vsm;

import java.util.Arrays;
import java.util.List;
import bin.Config;

public class Word2Vec {
	
	public static float[] createVecSum(List<String> text){
		
		float[] sum = new float[Config.word2Vec_dim];
		Arrays.fill(sum, 0);		
		
		for (String word : text) {
			try {
				float[] vector = Config.word2vec.getWordVector(word);
				
				if (vector == null) {
					continue;
				}
				else {
					for (int i = 0; i < vector.length; i++) {
						sum[i] = sum[i] + vector[i];
					}			
				}				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e);
				System.out.println(word);
			}			
			
			
		}		
		return sum;
	}
		
	public static float[] createVecCentroid(List<String> text){
		
		float[] centroid = new float[Config.word2Vec_dim];
		Arrays.fill(centroid, 0);
		
		for (String word : text) {
			try {
				float[] vector = Config.word2vec.getWordVector(word);				
				if (vector == null) {
					continue;
				}
				else {
					for (int i = 0; i < vector.length; i++) {
						centroid[i] = centroid[i] + vector[i];
					}			
				}				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e);
				System.out.println(word);
			}									
		}
		
		for (int i = 0; i < centroid.length; i++) {
			centroid[i] = centroid[i] / (float) Config.word2Vec_dim;
		}
		
		return centroid;
	}
}
