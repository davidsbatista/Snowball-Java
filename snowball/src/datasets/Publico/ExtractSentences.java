package datasets.Publico;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.PortuguesePOSTagger;

public class ExtractSentences {
	
	public static void generatePublicoXML(String xmlFile) throws Exception {		
		System.out.println("Extracting sentences from publico");
		LinkedList<Article> articles = datasets.Publico.ReadXML.returnWithTags(xmlFile);				
		BufferedWriter output_sentences = new BufferedWriter(new FileWriter(new File("sentences.txt")));
		int processed = 0;
		int id = 0;
		for (Article article : articles) {
			processed += 1;
			if (processed % 1000 == 0) System.out.println(processed+"/"+articles.size());
			String date = article.date;
			String text = cleanArticle(article.text);
			for (String s : PortuguesePOSTagger.sent.sentDetect(text)) {				
				output_sentences.write(String.valueOf(id) + '\t' + date + '\t' + s.trim().replaceAll("\n", " ") + '\n');
				id++;
			}
		}
		output_sentences.close();
	}
	
	public static void generatePublico(String txtFile) throws Exception {		
		System.out.println("Extracting sentences from publico");
		BufferedReader br = new BufferedReader(new FileReader(new File(txtFile)));			
		BufferedWriter output_sentences = new BufferedWriter(new FileWriter(new File("sentences.txt")));
		int processed = 0;
		int id = 0;
		String line = null;
		String[] document = null;
		String date = null;
		String category = null;
		String url = null;
		String title = null;
		String text = null;
		while((line = br.readLine())!= null) {
			document = line.split("\t");
			if (document.length==5) {
				category = document[0];
				date = document[1]; 
				url = document[2];
				title = document[3];
				text = document[4];				
			}
			else if (document.length==4) {
				category = document[0];
				date = document[1]; 
				url = document[2];				
				text = document[3];
			}
			else {
				System.out.println(line);
				continue;				
			}
			
			for (String s : PortuguesePOSTagger.sent.sentDetect(text)) {				
				output_sentences.write(String.valueOf(id) + '\t' + date + '\t' + s.trim().replaceAll("\n", " ") + '\n');
				id++;
			}
			processed += 1;
			if (processed % 1000 == 0) System.out.println("Processed " + processed);
		}
		output_sentences.close();
	}
	
	/*
	public static void generatePublico(String xmlFile) throws InvalidFormatException, FileNotFoundException, IOException {		
		System.out.println("Extracting sentences from publico");
		LinkedList<Article> articles = datasets.Publico.ReadXML.returnWithTags("/home/dsbatista/relations-minhash/publico.pt/publico-10-years-all.xml");
		Writer sentences = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("publico-sentences.txt"), "UTF8"));
		final TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
		final SentenceModel SENTENCE_MODEL = new IndoEuropeanSentenceModel();
		int id = 0;
		Iterator<Article> iterator = articles.iterator();
		while (iterator.hasNext()) {
			Article a = iterator.next();
			String date = a.date;
			String text = a.getText();				
			text = text.replaceAll("&quot;","\"");			
			List<String> tokenList = new ArrayList<String>();
			List<String> whiteList = new ArrayList<String>();
			Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(text.toCharArray(),0,text.length());
			tokenizer.tokenize(tokenList,whiteList);			
			String[] tokens = new String[tokenList.size()];
			String[] whites = new String[whiteList.size()];
			tokenList.toArray(tokens);
			whiteList.toArray(whites);
			int[] sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens,whites);			
			int sentStartTok = 0;
			int sentEndTok = 0;
				
			for (int i = 0; i < sentenceBoundaries.length; ++i) {
				//System.out.println("SENTENCE "+(i+1)+": ");
			    sentEndTok = sentenceBoundaries[i];			    
			    StringBuffer sentence =  new StringBuffer();
			    for (int j=sentStartTok; j <= sentEndTok; j++) {
			        sentence.append(tokens[j]+whites[j+1]);
			    }			    
			    sentStartTok = sentEndTok+1;
			    Pattern pattern = Pattern.compile("<[^>]*>[^<]+</[^>]+>");
				Matcher matcher = pattern.matcher(sentence);
				sentences.write(date + "\t" + sentence + "\n");
			    while (matcher.find()) {
			    	String type1 = matcher.group();
			    	String after1 = sentence.substring(matcher.end());
			    	Matcher matcher2 = pattern.matcher(after1);
				    while (matcher2.find()) {
				    	String type2 = matcher2.group();
						String before = sentence.substring(0,matcher.end()).replaceAll("<[^>]+>","");
						String after = sentence.substring(matcher.end()+matcher2.start()).replaceAll("<[^>]+>","");
						String between = sentence.substring(matcher.end(),matcher.end()+matcher2.start()).replaceAll("<[^>]+>","");
						after = between + " " + after;		                   
						before = before + " " + between;
		                before = before.replaceAll(" +", " ").trim();
		                after = after.replaceAll(" +", " ").trim();
		                between = between.replaceAll(" +", " ").trim();		                
		                type1 = type1.replaceAll(" ","_");
		                type2 = type2.replaceAll(" ","_");
						//processExample(before,after,between,date+"_"+String.valueOf(id)+"_"+type1+"-"+type2,out);
				  	}
				 }  
			    id++;
			}
		}
		//out.close();
		sentences.close();
	}
	*/
	
	/*
	 * Cleans the article so that it is valid XML and doesn't contain unseen characters/words by the POS-tagger   
	 */
	static String cleanArticle(String text) {
		//apply regular expressions to make valid XML
		text = text.replaceAll("&","&amp;");
		
		//remove unknown characters by CINTIL
		text = text.replaceAll("–","-");
		text = text.replaceAll("“\\s","\\\" ").replaceAll("\\s”\\."," \\\"\\.").replaceAll("\\s”,"," \\\",").replaceAll("\\s”\\."," \\\"\\.").replaceAll(" ” "," \\\" ");
		
		//fix some REMBRANDT stupid bugs
		Pattern regex = Pattern.compile("<LOCAL><ORGANIZACAO>([^<]+)</ORGANIZACAO>");
		Matcher matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll("<ORGANIZACAO>"+matcher.group(1)+"</ORGANIZACAO>");
		
		regex = Pattern.compile("<LOCAL><PESSOA>([^<]+)</PESSOA>");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll("<PESSOA>"+matcher.group(1)+"</PESSOA>");

		regex = Pattern.compile("<ORGANIZACAO><PESSOA>([^<]+)</PESSOA>");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll("<PESSOA>"+matcher.group(1)+"</PESSOA>");
		
		//replaces occurences of  "... qq coisa .É ..." replace to "... qq coisa. É ..." this occurred due to scraping errors			
		regex = Pattern.compile(" \\.([A-Za-z]+)");
		matcher = regex.matcher(text);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		while (matcher.find()) {
			found = true;
			matcher.appendReplacement(sb, ". "+matcher.group(1));
		}
		if (found) {
			matcher.appendTail(sb);
			text = sb.toString();
		}
		
		 /* O sentence dectector separar nomes como os abaixo, em duas frases:
		 * <PESSOA>Presidente George W. Bush</PESSOA>
		 * <PESSOA>H. David Politzer</PESSOA>
		 * <PESSOA>D. Quixote</PESSOA>
		 * <PESSOA>J. Craig Venter Institute</PESSOA>
		 * <PESSOA>Prof. Doutor Aníbal Cavaco Silva</PESSOA>.
		 * <ORGANIZACAO>Escola Secundária Dr. Manuel Candeias Gonçalves</ORGANIZACAO>
		 * <PESSOA>C.G. Jung</PESSOA>
		 * mais exemplos: grep -oP '<[^/a-z>]*>[^>]+\s[A-Za-z]+\.[^>]+</[^>]+>' publico-10-anos.xml
		 * 
		 *  retirar o "." da letra capitalizada no meio
		 */
		/*
		MATCHER:<PESSOA>Valter D. Longo</PESSOA>
		REPLACE:<PESSOA>Valter D Longo</PESSOA>
		MATCHER:<PESSOA>António M. Hespanha</PESSOA>
		MATCHER:<PESSOA>Richard R. Berhringer</PESSOA>
		REPLACE:<PESSOA>Richard R Berhringer</PESSOA>
		MATCHER:<PESSOA>Mr. Clinton</PESSOA>
		REPLACE:<PESSOA>Mr Clinton</PESSOA>
		MATCHER:<PESSOA>George W. Bush</PESSOA>
		MATCHER:<PESSOA>George W Bush</PESSOA>
		*/
		regex = Pattern.compile("(<[^/a-z>]*>[^>]+\\s?[A-Za-z]+)\\.([^>]+</[^>]+>)");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll(matcher.group(1) + matcher.group(2));
		
		/*
		MATCHER:<PESSOA>M. Night Shyamalan</PESSOA>
		REPLACE:<PESSOA>M Night Shyamalan</PESSOA>
		MATCHER:<PESSOA>D. Maria de Noronha</PESSOA>
		REPLACE:<PESSOA>D Maria de Noronha</PESSOA>
		MATCHER:<LOCAL>Av. da República</LOCAL>
		REPLACE:<LOCAL>Av da República</LOCAL>
		*/
		regex = Pattern.compile("(<[^/a-z>]*>[A-Za-z]+)\\.([^>]+</[^>]+>)");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll(matcher.group(1) + matcher.group(2));
		
		/*
		MATCHER: <PESSOA>AD. Miller</PESSOA>
		REPLACE: <PESSOA>AD Miller</PESSOA>
		MATCHER: <PESSOA>CG. Jung</PESSOA>
		REPLACE: <PESSOA>CG Jung</PESSOA>
		MATCHER: <PESSOA>JK. Rowling</PESSOA>
		REPLACE: <PESSOA>JK Rowling</PESSOA>
		MATCHER: <PESSOA>BB. King</PESSOA>
		REPLACE: <PESSOA>BB King</PESSOA>
		*/
		regex = Pattern.compile("(<[^/a-z>]*>[A-Z]+)\\.([^>]+</[^>]+>)");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll(matcher.group(1) + matcher.group(2));			
		
		/*
		<PESSOA>Paul W.S Anderson</PESSOA>		
		<PESSOA>arquitectos C.F. Møller</PESSOA>
		<PESSOA>E T. A Hoffmann</PESSOA>
		<ORGANIZACAO>IAT.H,SA</ORGANIZACAO>
		<LOCAL>Convento de N. S da Estrela</LOCAL>
		<PESSOA>Ronald Reagan Presidente e George H. W Bush</PESSOA>
		<ORGANIZACAO>Fundação T.J. Martell</ORGANIZACAO>>		
		<PESSOA> Dr. João Brito Camacho</PESSOA>
		<PESSOA> C.J. Jordan</PESSOA>
		<PESSOA>J.R.R. Tolkien</PESSOA>
		<PESSOA>R.I.P. Batman</PESSOA>
		<PESSOA>Carlos H. C. Silva</PESSOA>
		<LOCAL>Convento de N. S. da Estrela</LOCAL>
		*/
		
		/*
		<PESSOA>Fernando JB. Martinho</PESSOA>
		<PESSOA>ministro GK. Pillai</PESSOA>
		<PESSOA>vice-presidente da AT. Kerney</PESSOA>
		<ORGANIZACAO>Fundação TJ. Martell</ORGANIZACAO
		*/

		//42.500 euros
		regex = Pattern.compile("(<[^/a-z>]*>[^<]+)([0-9]+)\\.([0-9]+)(\\s[^<]+</[^>]+>)");
		matcher = regex.matcher(text);
		while (matcher.find()) text = matcher.replaceAll(matcher.group(1) + matcher.group(2) + ' ' + matcher.group(3) + matcher.group(4));		
		text = text.replaceAll("Prç.", "Prç").replaceAll("Dr.", "Dr");
		
		return text;
	}
}
