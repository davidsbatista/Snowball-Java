package nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import utils.Pair;

public class PortuguesePoSTagger {
	
	static POSModel model = null;
	static TokenizerModel tModel = null; 
	static SentenceModel sModel = null;
	static POSTaggerME tagger = null; 
	static TokenizerME token = null;
	public static SentenceDetector sent = null;
	static String RESOURCES = "/home/dsbatista/resources/";
	
	public static void initialize() throws InvalidFormatException, FileNotFoundException, IOException {
		
        /* with VPP tag */
        model = new POSModelLoader().load(new File(RESOURCES + "pt.postaggerVerbPP.model"));
        tModel = new TokenizerModel(new FileInputStream(RESOURCES + "pt.tokenizerVerbPP.model")); 
        sModel = new SentenceModel(new FileInputStream(RESOURCES + "pt.sentDetectVerbPP.model"));                
        tagger = new POSTaggerME(model); 
        token = new TokenizerME(tModel);
        sent = new SentenceDetectorME(sModel);
	}
	
	public static Pair<String[],String[]> tagSentence(String sentence){				
		String[] tokens = token.tokenize(sentence.trim());
		String[] pos_tags = tagger.tag(tokens);			
		Pair<String[], String[]> p = new Pair<String[], String[]>(tokens, pos_tags);
		return p;		
	}
	
	public static List<String> extractRVBPatterns(String text) {
		
		String[] sourcePOS = posTags(text.replaceAll("<[^>]+>",""));
		String[] sourceTokens = tokenize(text.replaceAll("<[^>]+>",""));
		List<String> set_patterns = new ArrayList<String>();
		if (sourcePOS.length==sourceTokens.length) {					
			for ( int i = 0 ; i < sourceTokens.length; i++ ) {		
				if ( sourcePOS[i].startsWith("verb") || sourcePOS[i].startsWith("pp") ) { //verb pode estar também no partícpio passado
					boolean pp_verb  = false;
					if (sourcePOS[i].startsWith("pp")) pp_verb = true;
					String pattern = null;
					pattern = sourceTokens[i].toLowerCase();
					
					//este primero if trata dos casos em que o verbo é um verbo auxilar: "ser", "estar", "ter", "ir", "haver", etc.
					//se o próximo token for um verbo também, ver se a sequência de tokens é um padrão ReVerb				
					if (( i+1 < sourceTokens.length-1 ) && (sourcePOS[i+1].startsWith("verb") || sourcePOS[i+1].startsWith("pp"))) {					
						pattern += "_" + sourceTokens[i+1].toLowerCase();
						int j = i;
						//ReVerb inspired: um verbo, seguido de vários nomes, adjectivos ou adverbios, terminando numa preposição.					
						if (i+2 < sourceTokens.length - 2) {	  			
							j = j+2;
							while ( ((j < sourceTokens.length - 2)) && ( 
									sourcePOS[j].startsWith("adverb") || 
									sourcePOS[j].startsWith("adjective") || 
									sourcePOS[j].startsWith("noun") ||
									sourcePOS[j].startsWith("pronoun") ))  {
							pattern += "_" + sourceTokens[j].toLowerCase();
							j++;
							}
						}
						i=j;
					}				
					else {
						//ReVerb inspired: um verbo, seguido de vários nomes, adjectivos ou adverbios, terminando numa preposição.					
						if (i < sourceTokens.length - 2) {	  			
							int j = i+1;
							while ( ((j < sourceTokens.length - 2)) && ( 
									sourcePOS[j].startsWith("adverb") || 
									sourcePOS[j].startsWith("adjective") || 
									sourcePOS[j].startsWith("noun") ||
									sourcePOS[j].startsWith("pronoun") ))  {
							pattern += "_" + sourceTokens[j].toLowerCase();
							j++;					
							}
							i=j;
						}
					}
					if ( sourcePOS[i].startsWith("preposition") || sourcePOS[i].startsWith("determiner")) {
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
		String whitespaceTokenizerLine[] = token.tokenize(text);
		return whitespaceTokenizerLine;		
	}
	
	public static String[] posTags(String text) {
		String tags[] = null;
		for (String s: sent.sentDetect(text)) {			
			String whitespaceTokenizerLine[] = token.tokenize(s);			
			String[] mTags = tagger.tag(whitespaceTokenizerLine);
			POSSample sample = new POSSample(whitespaceTokenizerLine, mTags);
			String sentence = sample.toString();
			String pairs[] = sentence.split(" ");
			tags = new String[pairs.length];
			for ( int i = 0; i < pairs.length; i++) {				
				tags[i] = pairs[i].substring(pairs[i].indexOf("_")+1);				 
			}
		}		
		return tags;
	}	
}
