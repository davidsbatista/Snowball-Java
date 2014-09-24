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

import edu.northwestern.at.utils.CharUtils;
import edu.northwestern.at.utils.corpuslinguistics.adornedword.AdornedWord;
import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.DefaultLemmatizer;
import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.Lemmatizer;
import edu.northwestern.at.utils.corpuslinguistics.lexicon.Lexicon;
import edu.northwestern.at.utils.corpuslinguistics.partsofspeech.PartOfSpeechTags;
import edu.northwestern.at.utils.corpuslinguistics.postagger.DefaultPartOfSpeechTagger;
import edu.northwestern.at.utils.corpuslinguistics.postagger.PartOfSpeechTagger;
import edu.northwestern.at.utils.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.utils.corpuslinguistics.sentencesplitter.SentenceSplitter;
import edu.northwestern.at.utils.corpuslinguistics.spellingstandardizer.DefaultSpellingStandardizer;
import edu.northwestern.at.utils.corpuslinguistics.spellingstandardizer.SpellingStandardizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.DefaultWordTokenizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.PennTreebankTokenizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.WordTokenizer;
import edu.northwestern.at.utils.CharUtils;
import edu.northwestern.at.utils.corpuslinguistics.adornedword.AdornedWord;
import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.DefaultLemmatizer;
import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.Lemmatizer;
import edu.northwestern.at.utils.corpuslinguistics.lexicon.Lexicon;
import edu.northwestern.at.utils.corpuslinguistics.partsofspeech.PartOfSpeechTags;
import edu.northwestern.at.utils.corpuslinguistics.postagger.DefaultPartOfSpeechTagger;
import edu.northwestern.at.utils.corpuslinguistics.postagger.PartOfSpeechTagger;
import edu.northwestern.at.utils.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.utils.corpuslinguistics.sentencesplitter.SentenceSplitter;
import edu.northwestern.at.utils.corpuslinguistics.spellingstandardizer.DefaultSpellingStandardizer;
import edu.northwestern.at.utils.corpuslinguistics.spellingstandardizer.SpellingStandardizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.DefaultWordTokenizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.PennTreebankTokenizer;
import edu.northwestern.at.utils.corpuslinguistics.tokenizer.WordTokenizer;

public class EnglishPoSTaggerMorphAdoner {
	
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
	
	public static List<String> ExtractReVerb(String source) {
				
		String auxPOS[] = EnglishNLP.adornText(source,1).split(" +");
		String normalized[] = EnglishNLP.adornText(source,3).split(" +");
		String aux[] = EnglishNLP.adornText(source,0).split(" +");
        
		List<String> set = new ArrayList<String>();
        
		for ( int i = 0 ; i < aux.length; i++ ) {        	
        	source = (i == 0) ? aux[i] : source + " " + aux[i];             
        	if ( auxPOS.length == normalized.length && auxPOS.length == aux.length ) {              
        		if ( auxPOS[i].startsWith("v") ) { 
        			set.add(normalized[i] + "_" + ( i < aux.length - 1 ? normalized[i+1] + "_" : "" ));
        			if ( !normalized[i].equals("be") && !normalized[i].equals("have") && auxPOS[i].equals("vvn") ) set.add(normalized[i] + "_VVN_");                       
        			//if ( !normalized[i].equals("be") && !normalized[i].equals("have") ) set.add(normalized[i] + "_");                      
                                  
        			// Passive voice detection
        			if (i < aux.length - 4) {
        				if ( (normalized[i].equals("have") && normalized[i+1].equals("be") && auxPOS[i+2].equals("vvn") && 
        					 (auxPOS[i+3].startsWith("pp") || auxPOS[i+3].equals("p-acp") || auxPOS[i+3].startsWith("pf") || 
        					  auxPOS[i+3].startsWith("pc-acp") || auxPOS[i+3].startsWith("acp")))) {
        					//set.add(normalized[i] + "_" + normalized[i+1] + "_" + normalized[i+2] +  "_" + normalized[i+3] + "_PASSIVE");
        					//set.add("_PASSIVE_");
    					}
    				}
        			
        			if (i < aux.length - 3) {
        				if ( i > 0 && (normalized[i-1].equals("have") || normalized[i-1].equals("be"))) continue;
        				if (((normalized[i].equals("have") || normalized[i].equals("be")) && auxPOS[i+1].equals("vvn") && (auxPOS[i+2].startsWith("pp") || auxPOS[i+2].equals("p-acp") || auxPOS[i+2].startsWith("pf") || auxPOS[i+2].startsWith("pc-acp") || auxPOS[i+2].startsWith("acp")))) {
        					//set.add(normalized[i] + "_" + normalized[i+1] + "_" + normalized[i+2] + "_PASSIVE");
        					//set.add("_PASSIVE_");
    					}
    				}
        			
        			//ReVerb inspired pattern: a verb, followed by:  nouns, adjectives or adverbs, ending in a proposition
        			if (i < aux.length - 2) {
        				String pattern = normalized[i];
                        int j = i + 1;
                        if ( j < aux.length && auxPOS[j].startsWith("pc-acp") || auxPOS[j].startsWith("acp")) {
                        	pattern += "_" + normalized[j++];
                        }                       
                        if ( j < aux.length && auxPOS[j].startsWith("av")) {
                        	pattern += "_" + normalized[j++];
                        }
                        while ( (j < aux.length - 2) && (auxPOS[j].startsWith("av") || // adverbs
								 auxPOS[j].equals("d") || auxPOS[j].startsWith("av-d") || auxPOS[j].equals("dc")|| auxPOS[j].equals("dg") || auxPOS[j].equals("ds") || auxPOS[j].equals("dx") || auxPOS[j].equals("n2-dx") || //determiners                                                                                      
                                 auxPOS[j].startsWith("j") || //adjectives
                                (auxPOS[j].startsWith("n") && !auxPOS[j].startsWith("nu")) || //nouns 
                                 auxPOS[j].startsWith("pi") || auxPOS[j].startsWith("po") || auxPOS[j].startsWith("pn") || auxPOS[j].startsWith("px"))) { //pronoun
                                 pattern += "_" + normalized[j];
                                 j++;                            
                                    }
                                    if ( ( j < aux.length ) && (auxPOS[j].startsWith("pp") || auxPOS[j].equals("p-acp") || auxPOS[j].startsWith("pf") || auxPOS[j].startsWith("pc-acp") || auxPOS[j].startsWith("acp"))) {
                                                    pattern += "_" + normalized[j];
                                    }
                                    set.add(pattern + "_RVB");
                                    
                                    // negation detection 
                                    if ( (i - 1 > 0) && ( normalized[i-1].equals("not") ||
                                                                  normalized[i-1].equals("neither") ||
                                                                  normalized[i-1].equals("nobody") ||
                                                                  normalized[i-1].equals("no") ||
                                                                  normalized[i-1].equals("none") ||
                                                                  normalized[i-1].equals("nor") ||
                                                                  normalized[i-1].equals("nothing") ||
                                                                  normalized[i-1].equals("nowhere") ||
                                                                  normalized[i-1].equals("never"))) set.add(normalized[i-1] + "_" + pattern + "_RVB_");                                                                 
                            }
                        }
                    }
            }
        return set;
     }
			
				
	
	
	public static String[] tokenize(String text){
		String whitespaceTokenizerLine[] = _tokenizer.tokenize(text);
		return whitespaceTokenizerLine;		
	}
}
