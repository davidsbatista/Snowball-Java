package bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nlp.PortuguesePoSTagger;
import nlp.PortugueseTokenizer;
import nlp.Stopwords;
import tuples.Seed;
import vsm.VectorSpaceModel;
import word2vec.com.ansj.vec.Word2VEC;

public class Config {
	
	public static PortugueseTokenizer tokenizer;	
	public static PortuguesePoSTagger tagger;
	public static VectorSpaceModel vsm = null;
	public static String e1_type = null;
	public static String e2_type = null;	
	public static Map<String,Float> parameters = new HashMap<String,Float>();
	public static Set<Seed> seedTuples = new HashSet<Seed>();
	public static String clusterType = null;	
	public static Word2VEC word2vec = null;
	public static int word2Vec_dim;	
	
	public static boolean useWord2Vec = true;	
	public static boolean extract_ReVerb = false;
	public static boolean useDBSCAN = false;
	
	/* Word2Vec configuration */
	public static boolean useReverb = false;
	public static boolean useMiddleSum = true;
	
	/* Represent tuples as sum of Word2Vec vectors
	 * or use the centroid of all vectors
	 */
	public static boolean useSum = true;
	public static boolean useCentroid = false;
		
	public static void init(String configFile, String sentencesFile, String stopwords, String vectors, String word2vecmodelPath) throws IOException {		
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader( new File(configFile)) );
			String line = null;
			Float value = null;
			String parameter = null;
			try {
				while ( ( line = f.readLine() ) != null) {
					if (line.isEmpty() || line.startsWith("#")) continue;
					parameter = line.split("=")[0];
					try {
						value = Float.parseFloat(line.split("=")[1]);
					} catch (Exception e) {
						System.out.println(line);
						e.printStackTrace();
						System.exit(0);
					}
					parameters.put(parameter, value);
				}
				
			} catch (NumberFormatException e) {
				System.out.println("Not a float");
				e.printStackTrace();
				System.exit(0);
				
			} catch (IOException e) {
				System.out.println("I/O error reading file");
				e.printStackTrace();
				System.exit(0);
			}
		} catch (FileNotFoundException e1) {
			System.out.println("paramters.cfg not found");
			// set default values
			parameters.put("occ_both_directions",new Float(0));			
			parameters.put("max_tokens_away",new Float(8));
			parameters.put("min_tokens_away",new Float(1));
			parameters.put("context_window_size",new Float(5));
			parameters.put("number_iterations",new Float(3.0));	
			parameters.put("min_degree_match",new Float(0.6));
			parameters.put("min_tuple_confidence",new Float(0.8));
			parameters.put("min_pattern_support",new Float(2));
			parameters.put("weight_left_context",new Float(0.2));
			parameters.put("weight_middle_context",new Float(0.6));
			parameters.put("weight_right_context",new Float(0.2));	
			parameters.put("wUpdt",new Float(0.5));
			parameters.put("DBScan_min_points", new Float(1));
			parameters.put("wUpdt",new Float(0.5));
			parameters.put("wNeg",new Float(0.5));
			parameters.put("wUnk",new Float(0.5));			
		}
		
		// initialize a tokenizer and load stopwords
		tokenizer = new PortugueseTokenizer();		
		System.out.print("Loading stopwords ...");		
		try {
			Stopwords.loadStopWords(stopwords);
		} catch (IOException e) {
			System.out.println("Stopwords file not found!");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("done");
		
		if (Config.extract_ReVerb==true) {
			PortuguesePoSTagger.initialize();
		}
		
		// load word2vec model
		word2vec = new Word2VEC();
		System.out.print("Loading word2vec model... ");
		word2vec.loadGoogleModel(word2vecmodelPath);			
		System.out.println(word2vec.getWords() + " words loaded");
		word2Vec_dim = word2vec.getSize();
	
		// calculate vocabulary term overall frequency
		try {
			generateTF(sentencesFile);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}
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
						if (Config.parameters.get("occ_both_directions")==1) {
							seed = new Seed(e2, e1);
							seedTuples.add(seed);
						}
						
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

	// Count individual terms frequency over whole document collection
	static void generateTF(String sentencesFile) throws IOException, FileNotFoundException, ClassNotFoundException {
		VectorSpaceModel vsm;
		File f = new File("vsm.obj");
		if (!f.exists()) {
			// scan all sentences, tokenize, and calculate TF
			System.out.println("Calculating TF for each term");			
			vsm = new VectorSpaceModel(sentencesFile);
			Config.vsm = vsm;
			System.out.println("TF-IDF vocabulary size: " + Config.vsm.term_document_frequency.keySet().size());
			
			try {
				// save to disk
				FileOutputStream out = new FileOutputStream("vsm.obj");
				ObjectOutputStream oo = new ObjectOutputStream(out);
				oo.writeObject(vsm);
				oo.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			// load already calculated TF
			System.out.println("Loading already calculated TF from disk");
			FileInputStream in = new FileInputStream("vsm.obj");
			ObjectInputStream objectInput = new ObjectInputStream(in);
			vsm = (VectorSpaceModel) objectInput.readObject();
			Config.vsm = vsm;
			System.out.println("TF-IDF vocabulary size: " + Config.vsm.term_document_frequency.keySet().size());						
			in.close();
			
		}
	}
}