package tests;

import org.jblas.FloatMatrix;

import vsm.TermsVector;
import bin.SnowballConfig;

public class TestWord2Vec {
	
	public static void main() {
		
		/* TEST WORD2VEC */ 
		System.out.println("Size   			: " + SnowballConfig.word2vec.getSize());
		System.out.println("Analogy			: " + SnowballConfig.word2vec.analogy("king", "man", "women"));
		System.out.println("Word Vector		: " + SnowballConfig.word2vec.getWordVector("king"));
		System.out.println("TopNSize		: " + SnowballConfig.word2vec.getTopNSize());		
		System.out.println("Distance		: " + SnowballConfig.word2vec.distance("charged"));		
		System.out.println();

		//cosine_similarity(model2['is']+model2['headquartered']+model2['in'],model2['is']+model2['based']+model2['in'])
		
		float[] v1 = SnowballConfig.word2vec.getWordVector("headquartered");
		float[] v2 = SnowballConfig.word2vec.getWordVector("in");
		
		float[] v3 = SnowballConfig.word2vec.getWordVector("based");
		float[] v4 = SnowballConfig.word2vec.getWordVector("in");
		
		/*
		float[] v3 = Config.word2vec.getWordVector("headquarters");
		float[] v4 = Config.word2vec.getWordVector("in");
		*/
				
		FloatMatrix m1 =  new FloatMatrix(v1);
		FloatMatrix m2 =  new FloatMatrix(v2);
		
		FloatMatrix m3 =  new FloatMatrix(v3);
		FloatMatrix m4 =  new FloatMatrix(v4);
		
		m1.addi(m2);
		m3.addi(m4);
				
		//System.out.println(m1);		
		//System.out.println(m3);		
		System.out.println("cos(): " + TermsVector.cosSimilarity(m1,m3));		
		System.exit(0);	
	}
}
