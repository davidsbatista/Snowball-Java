package nlp;

public class PortugueseTokenizer extends com.aliasi.tokenizer.RegExTokenizerFactory {
	
	private static final long serialVersionUID = 1L;
	
	private static String regex = "" +
	"((\\d+((/\\d+)+))" + "|" + 														/* dates or similar, e.g. 12/21/1 */
	",|\\(|\\)" + "|" +																	/* characters to be tokenized: , */
	"([\\d+\\p{L}._-]+?@[\\d+\\p{L}._-]+?\\.\\p{L}+)" + "|" + 							/* emails */                              								  
	"(\\d+\\:\\d+(\\:\\d+)?)" + "|" + 													/* the time, e.g. 12:12:2 */
    "(\\d+(([.]?[oaºª°])+))" + "|" + 													/* ordinal numbers, e.g. 12.o */	                              
    "(\\p{L}+(-\\p{L}+)+)" + "|" + 														/* tokens like dá-lo-à */
    "(\\p{L}+\\'\\p{L}+)" + "|" + 														/* word connected by ' */
    "(\\p{L}+)" + "|" + 																/* word characters */
    "(\\p{L}+\\.?[ºª°]\\.?)" + "|" +													/* ordinals */
    "(\\d+(([.,]\\d+)*\\%?)?)" + "|" + 													/* numbers */
    "((((http(s?))|(ftp)|(gopher))://|www)[\\p{L}\\d_=#%?&./~:-]+\\p{L})" + "|" + 		/* urls */
    "(\\p{L}+\\.((com)|(org)|(net)|(pt))?))";											/* simplified urls */	                              

	public PortugueseTokenizer ( ) { super(regex); }

	public com.aliasi.tokenizer.Tokenizer tokenizer(String str) { return tokenizer(str.toCharArray(),0,str.length()); } 

	public String[] tokenize(char[] cs, int start, int length) { 
	   com.aliasi.tokenizer.Tokenizer tokenizer = tokenizer(cs,start,length);
	   return tokenizer.tokenize();
	}
	
	public static void test(){
		String[] sentences = {"A candidatura de Ferreira do Amaral, apoiada pelo PSD, \"considera o montante\" limite para a campanha \" curto \".",
							  "A candidatura presidencial de Jorge Sampaio considera que os limites de gastos com campanhas eleitorais \" são moderadores e úteis \"."};
		
		PortugueseTokenizer tf = new PortugueseTokenizer();
		for ( String str : sentences ) {
			System.out.println();
			for ( String token : tf.tokenize(str) ) System.out.println(token);
			}
	}

	public String[] tokenize(String str) { 
	  com.aliasi.tokenizer.Tokenizer tokenizer = tokenizer(str.toCharArray(),0,str.length());
	  return tokenizer.tokenize();
	} 
	 
	public static void main ( String args[] ) {
		System.out.println("Testing tokenization of the input parameters...");
		PortugueseTokenizer tf = new PortugueseTokenizer();
		for ( String str : args ) {
			System.out.println();
			for ( String token : tf.tokenize(str) ) System.out.println(token);
			}		
	}
}