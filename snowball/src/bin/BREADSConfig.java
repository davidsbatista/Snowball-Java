package bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nlp.EnglishPoSTagger;
import nlp.PortuguesePoSTagger;
import nlp.PortugueseTokenizer;
import nlp.Stopwords;
import tuples.Seed;
import word2vec.com.ansj.vec.Word2VEC;
import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.EnglishLemmatizer;

public class BREADSConfig {
	
	/* NLP related configuration */
	public static PortugueseTokenizer PTtokenizer;
	public static PortuguesePoSTagger tagger;
	
	public static EnglishLemmatizer EnglishLemm;
	public static String stopwords;
	
	public static String PoS_models_path;
	
	static String verbs[] = {"be","have"};
	public static List<String> aux_verbs = Arrays.asList(verbs);
	
	/* Seeds data structures */
	public static String e1_type = null;
	public static String e2_type = null;
	public static Set<Seed> seedTuples = new HashSet<Seed>();	
		
	/* Word2Vec related stuff */
	public static Word2VEC word2vec = null;
	public static int word2Vec_dim;
	public static String Word2VecModelPath;
	
	/* how to represent ReVerb patterns / relational words: sum,centroid */
	public static String single_vector;
	
	/* similarity between a sentence and extraction patterns: sum,centroid,all */
	public static String similarity;

	/* threshold similarity for clustering */
	public static double threshold_similarity;
	
	/* confidence threshold of an instance to used as seed */
	public static double instance_confidance;
	
	/* expand extraction patterns */
	public static boolean expand_patterns;
	
	/* how is the expansion made: single-vector, common-words */
	public static String expansion;
	
	/* number of words considered for expansion */	
	public static int top_k;
	
	/* minimum number of clustered instances to be considered a pattern */ 
	public static int min_pattern_support;	
	
	public static int max_tokens_away;
	public static int min_tokens_away;
	public static int context_window_size;
	
	/* iteration control parameters */
	public static int number_iterations;
	public static double wUpdt;
	public static boolean use_RlogF;
				
	public static void init(String parameters, String sentencesFile) throws Exception {		
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader( new File(parameters)) );
			String line = null;
			try {
				while ( ( line = f.readLine() ) != null) {					
					if (line.isEmpty() || line.startsWith("#")) continue;					
					if (line.startsWith("wUpdt")) wUpdt = Double.parseDouble(line.split("=")[1]);
					if (line.startsWith("number_iterations")) number_iterations = Integer.parseInt(line.split("=")[1]);
					if (line.startsWith("use_RlogF")) use_RlogF = Boolean.parseBoolean(line.split("=")[1]);					
					if (line.startsWith("min_pattern_support")) min_pattern_support = Integer.parseInt(line.split("=")[1]);					
					
					if (line.startsWith("max_tokens_away")) max_tokens_away = Integer.parseInt(line.split("=")[1]);					
					if (line.startsWith("min_tokens_away")) min_tokens_away = Integer.parseInt(line.split("=")[1]);
					if (line.startsWith("context_window_size")) context_window_size = Integer.parseInt(line.split("=")[1]);
					
					if (line.startsWith("expand_patterns")) BREADSConfig.expand_patterns = Boolean.parseBoolean(line.split("=")[1]);
					if (line.startsWith("expansion")) BREADSConfig.expansion = line.split("=")[1];
					if (line.startsWith("top_k")) BREADSConfig.top_k = Integer.parseInt(line.split("=")[1]);
					
					if (line.startsWith("single_vector")) BREADSConfig.single_vector = line.split("=")[1];
					if (line.startsWith("similarity")) BREADSConfig.similarity = line.split("=")[1];
					if (line.startsWith("threshold_similarity")) BREADSConfig.threshold_similarity = Double.parseDouble(line.split("=")[1]);
					if (line.startsWith("instance_confidance")) BREADSConfig.instance_confidance = Double.parseDouble(line.split("=")[1]);
					
					if (line.startsWith("stopwords")) BREADSConfig.stopwords = line.split("=")[1];
					if (line.startsWith("PoS_models_path")) BREADSConfig.PoS_models_path = line.split("=")[1];
					
					if (line.startsWith("word2vec_path")) BREADSConfig.Word2VecModelPath = line.split("=")[1];
				}				
			} catch (IOException e) {
				System.out.println("I/O error reading paramters.cfg");
				e.printStackTrace();
				System.exit(0);
			}
		} catch (FileNotFoundException e1) {
			System.out.println("paramters.cfg not found");
			System.exit(0);
		}
			
		// Initialize a Tokenizer and load Stopwords		
		PTtokenizer = new PortugueseTokenizer();		
		EnglishLemm = new EnglishLemmatizer();		
		System.out.print("Loading stopwords ...");
		try {
			Stopwords.loadStopWords(BREADSConfig.stopwords);
		} catch (IOException e) {
			System.out.println("Stopwords file not found!");
			e.printStackTrace();
			System.exit(0);
		}		
		System.out.println("done");
		
		// Load Word2vec model
		word2vec = new Word2VEC();
		System.out.print("Loading word2vec model... ");
		word2vec.loadGoogleModel(BREADSConfig.Word2VecModelPath);			
		System.out.println(word2vec.getWords() + " words loaded");
		word2Vec_dim = word2vec.getSize();
		System.out.println("Vectors dimension: " + word2Vec_dim);
			
		// Load PoS-tagging models			
		//PortuguesePoSTagger.initialize();
		EnglishPoSTagger.initialize();
	}
		
	// Read seed instances from file
	static void readSeeds(String seedsFile){
		BufferedReader f = null;
		try {
			f = new BufferedReader(new FileReader( new File(seedsFile)));
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
			System.exit(0);
		}
		String line = null;
		String e1 = null;
		String e2 = null;
		Seed seed = null;
		try {
			while ( ( line = f.readLine() ) != null) {
				try {
					if (line.startsWith("#") || line.isEmpty()) continue;
					if (line.startsWith("e1")) e1_type = line.split(":")[1];
					else if (line.startsWith("e2")) e2_type = line.split(":")[1];					
					else {
						e1 = line.split(";")[0];
						e2 = line.split(";")[1];
						seed = new Seed(e1, e2);
						seedTuples.add(seed);						
					}					
				} catch (Exception e) {
					System.out.println("Error parsing: " + line);
					e.printStackTrace();
					System.exit(0);
				}				
			}
			f.close();
		} catch (IOException e) {
			System.out.println("I/O error");
			e.printStackTrace();
		}
	}
}