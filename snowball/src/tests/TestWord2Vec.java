package tests;

import org.jblas.FloatMatrix;

import vsm.TermsVector;
import bin.Config;

public class TestWord2Vec {
	
	public static void main() {
		
		/* TEST WORD2VEC */ 
		System.out.println("Size   			: " + Config.word2vec.getSize());
		System.out.println("Analogy			: " + Config.word2vec.analogy("rei", "homem", "mulher"));
		System.out.println("Word Vector		: " + Config.word2vec.getWordVector("rei"));
		System.out.println("TopNSize		: " + Config.word2vec.getTopNSize());		
		System.out.println("Distance		: " + Config.word2vec.distance("acusou"));		
		System.out.println();

		float[] v1 = Config.word2vec.getWordVector("chefe");
		float[] v2 = Config.word2vec.getWordVector("do");
		
		float[] v3 = Config.word2vec.getWordVector("presidente");
		float[] v4 = Config.word2vec.getWordVector("do");
				
		FloatMatrix m1 =  new FloatMatrix(v1);
		FloatMatrix m2 =  new FloatMatrix(v2);
		
		FloatMatrix m3 =  new FloatMatrix(v3);
		FloatMatrix m4 =  new FloatMatrix(v4);
		
		m1.addi(m2);
		m3.addi(m4);
				
		System.out.println(m1);		
		System.out.println(m3);		
		System.out.println("cos(): " + TermsVector.cosSimilarity(m1,m3));		
		System.exit(0);	
	}
}
