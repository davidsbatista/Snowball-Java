package tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import opennlp.tools.util.InvalidFormatException;

import nlp.EnglishPoSTagger;
import nlp.ReVerbPattern;

public class TestReVerbPatterns {
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {
		
		String token = "/home/dsbatista/OpenNLP_models/en-token.bin";
		String maxent = "/home/dsbatista/OpenNLP_models/en-pos-maxent.bin";
		String tagset = "/home/dsbatista/OpenNLP_models/universal-tagset.txt";
		
		EnglishPoSTagger.initialize(token, maxent, tagset);
		
		String text = "Hernandez did not graduated last month and will join the LPGA's developmental tour in the next few weeks"; 
				
		List<ReVerbPattern> patterns = EnglishPoSTagger.extractRVB(text);
		
		for (ReVerbPattern reVerbPattern : patterns) {			
			System.out.println(reVerbPattern.token_words);
			System.out.println(reVerbPattern.token_universal_pos_tags);
			System.out.println();
		}
	}

}
