package tests;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTester {
	
	static String a = "- 01 : The military transfers control of western province of to forces , the first province to be handed over ."; 
	
	//term = term.replaceAll(" (\\,|\\.|/\\(\\))? ([0-9]+)?.?(º|ª|%)?", "");
	static Pattern ptr = Pattern.compile("[0-9]+?|\\(|\\)|,|-+|:|\\.");
	
	public static void main(String[] args) {		
		List<String> terms = new LinkedList<String>();		
		for (String term : Arrays.asList(a.split("\\s+?"))) {
			Matcher matcher = ptr.matcher(term);
			System.out.println(term + '\t' + term.length() + '\t' + matcher.matches());
			if (!matcher.matches()) terms.add(term);			
		}
		System.out.println(terms);
	}
}