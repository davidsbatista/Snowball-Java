package nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PortugueseVerbNormalizer {
	
	static String file = "/home/dsbatista/resources/Label-Delaf_pt_v4_1.dic.utf8";	
	public static HashMap<String, List<String>> verbs = null; 	
	public static String[] ambigous = {"foi_a_RVB", "foi_de_RVB", "fora_de_RVB", "foram_de_RVB", "for_de_RVB", "foi_mais_de_RVB","fossem_de_RVB","fosse_a_RVB","fossem_em_RVB","fossem_sobre_RVB","fossem_para_RVB","fossem_por_RVB","fossem_de_RVB","fosse_com_RVB","fossem_de_RVB","tendo_a_RVB","tende_a_RVB","adia_em_RVB"};
     
	public static void initialize(){		
		System.out.print("Loading verbs... ");
		BufferedReader br = null;		
		verbs = new HashMap<String, List<String>>();
		String infitinitve;
		String conjugation;
		List<String> tmp = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
		        if ( (!line.startsWith("%")) && line.split("\\.")[1].startsWith("V") && (!line.split("\\.")[1].startsWith(".Vm"))) {
		        	infitinitve = line.split("\\.")[0].split(",")[1];
		        	conjugation = line.split("\\.")[0].split(",")[0];
		        	tmp = verbs.get(conjugation);
		        	if (tmp==null) {
		        		LinkedList<String> infinitives = new LinkedList<String>();
		        		infinitives.add(infitinitve);
		        		verbs.put(conjugation, infinitives);
		        	}
		        	else {
		        		tmp.add(infitinitve);
		        		verbs.put(conjugation, tmp);
		        	}
				}				
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}		
		System.out.println();
		System.out.println(verbs.size() + " verbs conjungations loaded");
	}
	
	public static String normalizeVerb(String verb){		
		List<String> normalized = verbs.get(verb);		
		if (normalized == null) return null;		
		if (normalized.size()==1) return normalized.get(0);				
		else if (normalized.size()==2) {			
            // ter e tender    -> ter     196 casos (tendo)
            if (normalized.get(0).equals("ter") && normalized.get(1).equals("tender")) return "ter";
            
            // fossar e ir     -> ir      119 casos
            if (normalized.get(0).equals("fossar") && normalized.get(1).equals("ir")) return "ir";

            // estar e estevar -> estar   115 casos
            if (normalized.get(0).equals("estar") && normalized.get(1).equals("estevar")) return "estar";

            // ser e seriar    -> ser     113 casos
            if (normalized.get(0).equals("ser") && normalized.get(1).equals("seriar")) return "ser";
            
            //vivar e viver   -> viver    40 casos
            if (normalized.get(0).equals("vivar") && normalized.get(1).equals("viver")) return "viver";

            //falar e falir   -> falar    37 casos
            if (normalized.get(0).equals("falar") && normalized.get(1).equals("falir")) return "falar";

            //segar e seguir  -> seguir   22 casos
            if (normalized.get(0).equals("segar") && normalized.get(1).equals("seguir")) return "seguir";

            //podar e poder   -> poder     8 casos
            if (normalized.get(0).equals("podar") && normalized.get(1).equals("poder")) return "poder";

            //unar e unir     -> unir      5 casos
            if (normalized.get(0).equals("unar") && normalized.get(1).equals("unir")) return "poder";

            //ir e iriar      -> ir        4 casos
            if (normalized.get(0).equals("ir") && normalized.get(1).equals("iriar")) return "ir";
            
            //ver e vestir    -> ver       4 casos
            if (normalized.get(0).equals("ir") && normalized.get(1).equals("iriar")) return "ir";
            
            //estar e estivar -> estar     4 casos
            if (normalized.get(0).equals("estar") && normalized.get(1).equals("estivar")) return "estar";
            
            //presidiar e presidir -> presidir 4 casos
            if (normalized.get(0).equals("presidiar") && normalized.get(1).equals("presidir")) return "presidir";
            
            //crer	criar
            if (normalized.get(0).equals("crer") && normalized.get(1).equals("criar")) return "criar";
            
            //adiar	adir
            if (normalized.get(0).equals("adiar") && normalized.get(1).equals("adir")) return "adiar";
		}
		return verb;
	}
}
