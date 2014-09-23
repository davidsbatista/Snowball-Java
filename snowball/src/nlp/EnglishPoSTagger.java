package nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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
	
	public static List<ReVerbPattern> tagSentence(String sentence){
		String[] tokens = _tokenizer.tokenize(sentence.trim());
		String[] PTB_POS = _posTagger.tag(tokens);
		String[] sourcePOS = new String[PTB_POS.length];
		
		for (int i = 0; i < PTB_POS.length; i++) {
			sourcePOS[i] = UniversalTagSet.convertTagsPTB.get(PTB_POS[i]);
		}
		
		List<String> token_words = new ArrayList<String>();
		List<String> token_universal_pos_tags = new ArrayList<String>();
		List<String> token_ptb_pos_tags = new ArrayList<String>();
		
		List<ReVerbPattern> patterns = new LinkedList<>();
		
		for (int j = 0; j < tokens.length; j++) {
			token_words.add(tokens[j].toLowerCase());
			token_ptb_pos_tags.add(PTB_POS[j]);
			token_universal_pos_tags.add(sourcePOS[j]);
		}
		
		ReVerbPattern p = new ReVerbPattern(token_words, token_universal_pos_tags, token_ptb_pos_tags);
		patterns.add(p);
		
		return patterns;		
	}
	
	
	public static List<ReVerbPattern> extractRVBPatterns(String text) {
		
		String[] sourceTokens = _tokenizer.tokenize(text.replaceAll("<[^>]+>",""));
		String[] PTB_POS = _posTagger.tag(sourceTokens);
		String[] sourcePOS = new String[PTB_POS.length];
		for (int i = 0; i < PTB_POS.length; i++) {
			sourcePOS[i] = UniversalTagSet.convertTagsPTB.get(PTB_POS[i]);
		}				
		List<ReVerbPattern> patterns = new LinkedList<>();		
		
		if (sourcePOS.length==sourceTokens.length) {					
			for ( int i = 0 ; i < sourceTokens.length; i++ ) {		
				if ( sourcePOS[i].startsWith("VERB")) {
					
					List<String> token_words = new ArrayList<String>();
					List<String> token_universal_pos_tags = new ArrayList<String>();
					List<String> token_ptb_pos_tags = new ArrayList<String>();
					
					token_words.add(sourceTokens[i].toLowerCase());
					token_universal_pos_tags.add(sourcePOS[i]);
					token_ptb_pos_tags.add(PTB_POS[i]);
															
					// Este primero if trata dos casos em que o verbo é um verbo auxilar: "ser", "estar", "ter", "ir", "haver", etc.
					// Se o próximo token for um verbo também, ver se a sequência de tokens é um padrão ReVerb				
					
					if (( i+1 < sourceTokens.length-1 ) && (sourcePOS[i+1].startsWith("VERB") || sourcePOS[i+1].startsWith("pp"))) {
						token_words.add(sourceTokens[i+1].toLowerCase());
						token_universal_pos_tags.add(sourcePOS[i]);
						token_ptb_pos_tags.add(PTB_POS[i]);
						
						int j = i;
						// ReVerb inspired: a VERB, followed by several NOUNS, ADJ or ADV, ending in a PREP					
						if (i+2 < sourceTokens.length - 2) {	  			
							j = j+2;
							while ( ((j < sourceTokens.length - 2)) && ( 
									sourcePOS[j].startsWith("ADV") || 
									sourcePOS[j].startsWith("ADJ") || 
									sourcePOS[j].startsWith("NOUN") ||
									sourcePOS[j].startsWith("PRON") ))  {							
							token_words.add(sourceTokens[i+1].toLowerCase());
							token_universal_pos_tags.add(sourcePOS[i]);
							token_ptb_pos_tags.add(PTB_POS[i]);
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
							//pattern += "_" + sourceTokens[j].toLowerCase();
							//pos_pattern += "_" + sourcePOS[i];
							token_words.add(sourceTokens[i].toLowerCase());
							token_universal_pos_tags.add(sourcePOS[i]);
							token_ptb_pos_tags.add(PTB_POS[i]);
							j++;					
							}
							i=j;
						}
					}
					if ( sourcePOS[i].startsWith("ADP") || sourcePOS[i].startsWith("DET")) {
						//pattern += "_" + sourceTokens[i].toLowerCase();
						//pos_pattern += "_" + sourcePOS[i];
						token_words.add(sourceTokens[i].toLowerCase());
						token_universal_pos_tags.add(sourcePOS[i]);
						token_ptb_pos_tags.add(PTB_POS[i]);
					}
					ReVerbPattern patternRVB = new ReVerbPattern(token_words,token_universal_pos_tags,token_ptb_pos_tags);
					patterns.add(patternRVB);
				}
			}
		}
		else {
			System.out.println("Error: different number of tokens and PoS-tags");
			System.exit(0);
		}		
		return patterns;
	}

	
	public static String[] tokenize(String text){
		String whitespaceTokenizerLine[] = _tokenizer.tokenize(text);
		return whitespaceTokenizerLine;		
	}
}
