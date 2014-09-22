package nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class UniversalTagSet {
	
	static HashMap<String, String> convertTagsPTB = new HashMap<String,String>();
	
	public static void init() throws IOException {
		
		String file = "models/universal-tagset.txt";		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {			
			String[] tags = line.split("\\t");
			convertTagsPTB.put(tags[0], tags[1]);	  
		}
		br.close();		
	}
	
	public static void convert(String tag) {
		
	}
}