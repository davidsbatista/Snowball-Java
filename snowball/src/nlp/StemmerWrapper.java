package nlp;

import ptstemmer.Stemmer;
import ptstemmer.exceptions.PTStemmerException;
import ptstemmer.implementations.OrengoStemmer;

public class StemmerWrapper {
		
	static Stemmer stemmer = null;
	
	public static void initialize() throws PTStemmerException{
		stemmer = new OrengoStemmer();
	    stemmer.enableCaching(1000);   //Optional
	    //stemmer.ignore(PTStemmerUtilities.fileToSet("data/namedEntities.txt"));  //Optional
	    		
	}
	
	public static String stem(String word) {
		return stemmer.getWordStem(word); 
	}
}
