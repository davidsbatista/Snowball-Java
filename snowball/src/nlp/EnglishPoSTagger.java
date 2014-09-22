package nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import utils.Pair;

public class EnglishPoSTagger {
	
	static POSTaggerME _posTagger = null;
	static Tokenizer _tokenizer = null;
			
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {		
		initialize();		
		UniversalTagSet.init();		
		List<String> patterns = extractRVBPatterns("are based on");
		
		for (String string : patterns) {
			System.out.println(string);
		}
	}
	
	public static void initialize() throws InvalidFormatException, FileNotFoundException, IOException {		
		System.out.println("Loading English PoS-tagger");
		
		_tokenizer = null;	
	   // Loading tokenizer model		   
	   final TokenizerModel tokenModel = new TokenizerModel( new FileInputStream(new File("models/en-token.bin")));		 
	   _tokenizer = new TokenizerME(tokenModel);
		 
       // Loading pos model
       final POSModel posModel = new POSModel( new FileInputStream(new File("models/en-pos-maxent.bin")));         
       _posTagger = new POSTaggerME(posModel);
       
       UniversalTagSet.init();
	}
	
	public static Pair<String[],String[]> tagSentence(String sentence){				
		String[] tokens = _tokenizer.tokenize(sentence.trim());
		String[] pos_tags = _posTagger.tag(tokens);			
		Pair<String[], String[]> p = new Pair<String[], String[]>(tokens, pos_tags);
		return p;		
	}
	
	public static List<String> extractRVBPatterns(String text) {
		
		String[] sourceTokens = _tokenizer.tokenize(text.replaceAll("<[^>]+>",""));
		String[] PTB_POS = _posTagger.tag(sourceTokens);
		String[] sourcePOS = new String[PTB_POS.length];
		for (int i = 0; i < PTB_POS.length; i++) {
			sourcePOS[i] = UniversalTagSet.convertTagsPTB.get(PTB_POS[i]);
		}
		
		/*
		for (int i = 0; i < sourceTokens.length; i++) {
			System.out.print(sourceTokens[i] + '\t');
			System.out.print(PTB_POS[i] + '\t');
			System.out.print(sourcePOS[i] + '\n');
		}
		*/
			
		List<String> set_patterns = new ArrayList<String>();
		if (sourcePOS.length==sourceTokens.length) {					
			for ( int i = 0 ; i < sourceTokens.length; i++ ) {		
				if ( sourcePOS[i].startsWith("VERB") || sourcePOS[i].startsWith("pp") ) { //verb pode estar também no partícpio passado
					boolean pp_verb  = false;
					if (sourcePOS[i].startsWith("pp")) pp_verb = true;
					String pattern = null;
					pattern = sourceTokens[i].toLowerCase();
					
					// Este primero if trata dos casos em que o verbo é um verbo auxilar: "ser", "estar", "ter", "ir", "haver", etc.
					// Se o próximo token for um verbo também, ver se a sequência de tokens é um padrão ReVerb				
					if (( i+1 < sourceTokens.length-1 ) && (sourcePOS[i+1].startsWith("VERB") || sourcePOS[i+1].startsWith("pp"))) {					
						pattern += "_" + sourceTokens[i+1].toLowerCase();
						int j = i;
						// ReVerb inspired: a VERB, followed by several NOUNS, ADJ or ADV, ending in a PREP					
						if (i+2 < sourceTokens.length - 2) {	  			
							j = j+2;
							while ( ((j < sourceTokens.length - 2)) && ( 
									sourcePOS[j].startsWith("ADV") || 
									sourcePOS[j].startsWith("ADJ") || 
									sourcePOS[j].startsWith("NOUN") ||
									sourcePOS[j].startsWith("PRON") ))  {
							pattern += "_" + sourceTokens[j].toLowerCase();
							j++;
							}
						}
						i=j;
					}				
					else {
						// ReVerb inspired: a VERB, followed by several NOUNS, ADJ or ADV, ending in a PREP					
						if (i < sourceTokens.length - 2) {	  			
							int j = i+1;
							while ( ((j < sourceTokens.length - 2)) && ( 
									sourcePOS[j].startsWith("ADV") || 
									sourcePOS[j].startsWith("ADJ") || 
									sourcePOS[j].startsWith("NOUN") ||
									sourcePOS[j].startsWith("PRON") ||
									sourcePOS[j].startsWith("DET")))  {
							pattern += "_" + sourceTokens[j].toLowerCase();
							j++;					
							}
							i=j;
						}
					}
					if ( sourcePOS[i].startsWith("ADP") || sourcePOS[i].startsWith("DET")) {
						pattern += "_" + sourceTokens[i].toLowerCase();
					}				
					//System.out.println("pattern: " + pattern);
					if (pp_verb) pattern += "_PP";				
					set_patterns.add(pattern);			
				}
			}
		}
		else {
			set_patterns.add("ERROR!");
		}
		return set_patterns;
	}

	
	public static String[] tokenize(String text){
		String whitespaceTokenizerLine[] = _tokenizer.tokenize(text);
		return whitespaceTokenizerLine;		
	}
}
