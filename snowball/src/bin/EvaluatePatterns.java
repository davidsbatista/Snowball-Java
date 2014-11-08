package bin;

import java.io.IOException;
import java.util.List;

import org.jblas.FloatMatrix;

import vsm.TermsVector;
import word2vec.com.ansj.vec.Word2VEC;

public class EvaluatePatterns {
	
	static String wikipedia = "/home/dsbatista/word2vec-read-only/vectors.bin";
    static String google_news = "/home/dsbatista/GoogleNews-vectors-negative300.bin";
    static String afp_apw = "/home/dsbatista/gigaword/word2vec/afp_apw_vectors.bin";
    static String model_afp_apw_xing = "/home/dsbatista/gigaword/word2vec/afp_apw_xing_vectors.bin";
	
	/* Word2Vec related stuff */
	public static Word2VEC word2vec = null;
	public static int word2Vec_dim;
	public static String Word2VecModelPath;
	
	public static void init() throws IOException {				
		// Load Word2vec model
		word2vec = new Word2VEC();
		System.out.print("Loading word2vec model... ");
		word2vec.loadGoogleModel(google_news);			
		System.out.println(word2vec.getWords() + " words loaded");
		word2Vec_dim = word2vec.getSize();
		System.out.println("Vectors dimension: " + word2Vec_dim);		
	}
		
	public static void comparePatterns(List<String> pattern1, List<String >pattern2, Word2VEC model) {		
	}
	

	/*
	['studied', 'journalism'] , ['journalism', 'professor', 'at']
	[[ 0.]]

	Google model
	['studied', 'journalism'] , ['journalism', 'professor', 'at']
	[[ 0.]]

	AFP,APW model
	['studied', 'journalism'] , ['journalism', 'professor', 'at']
	[[ 0.]]

	AFP,APW,XING model
	['studied', 'journalism'] , ['journalism', 'professor', 'at']
*/
	
	
	
	public static void test() {
		
		/* TEST WORD2VEC */ 
		System.out.println("Size   			: " + BREDSConfig.word2vec.getSize());
		System.out.println("Analogy			: " + BREDSConfig.word2vec.analogy("king", "man", "women"));
		System.out.println("Word Vector		: " + BREDSConfig.word2vec.getWordVector("king"));
		System.out.println("TopNSize		: " + BREDSConfig.word2vec.getTopNSize());		
		System.out.println("Distance		: " + BREDSConfig.word2vec.distance("charged"));		
		System.out.println();

		//cosine_similarity(model2['is']+model2['headquartered']+model2['in'],model2['is']+model2['based']+model2['in'])
		
		float[] v1 = BREDSConfig.word2vec.getWordVector("headquartered");
		//float[] v2 = SnowballConfig.word2vec.getWordVector("in");
		
		float[] v3 = BREDSConfig.word2vec.getWordVector("headquarters");
		//float[] v4 = SnowballConfig.word2vec.getWordVector("in");
		
		/*
		float[] v3 = Config.word2vec.getWordVector("headquarters");
		float[] v4 = Config.word2vec.getWordVector("in");
		*/
				
		FloatMatrix m1 =  new FloatMatrix(v1);
		//FloatMatrix m2 =  new FloatMatrix(v2);
		
		FloatMatrix m3 =  new FloatMatrix(v3);
		//FloatMatrix m4 =  new FloatMatrix(v4);
		
		//m1.addi(m2);
		//m3.addi(m4);
				
		//System.out.println(m1);		
		//System.out.println(m3);		
		System.out.println("cos(): " + TermsVector.cosSimilarity(m1,m3));		
		System.exit(0);	
	}
}
