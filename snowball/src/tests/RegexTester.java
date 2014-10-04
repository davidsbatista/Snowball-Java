package tests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTester {

	static String a = "O bailado \" <PESSOA>D. Quixote</PESSOA> \" é uma produção, na épica obra-prima de <PESSOA>Miguel Cervantes</PESSOA>, originalmente criada para o <LOCAL>Bolshoi</LOCAL> por <PESSOA>Marius Petipa</PESSOA> em 1869.";
	static String b = "O bailado \" <PESSOA>Presidente George W. Bush</PESSOA> \" é uma produção, na épica obra-prima de <PESSOA>Miguel Cervantes</PESSOA>, originalmente criada para o <LOCAL>Bolshoi</LOCAL> por <PESSOA>Marius Petipa</PESSOA> em 1869.";
	static String c = "O bailado \" <PESSOA>Dr. Fausto</PESSOA> \" é uma produção, na épica obra-prima de <PESSOA>Miguel Cervantes</PESSOA>, originalmente criada para o <LOCAL>Bolshoi</LOCAL> por <PESSOA>Marius Petipa</PESSOA> em 1869.";
	static String d = "O bailado \" <PESSOA>D. Fausto</PESSOA> \" é uma produção, na épica obra-prima de <PESSOA>Miguel Cervantes</PESSOA>, originalmente criada para o <LOCAL>Bolshoi</LOCAL> por <PESSOA>Marius Petipa</PESSOA> em 1869.";
	static String e = "Mas a questão permaneceu adormecida até ao concurso da <PESSOA>Miguel Prudêncio</PESSOA> e <PESSOA>Miguel Soares</PESSOA> que conquistaram os júris da <ORGANIZACAO>Fundação Bill</ORGANIZACAO> & <PESSOA>Melinda Gates</PESSOA>.";
	static String f = "Desta vez, as freguesias contempladas serão cinco: <LOCAL>São Sebastião da Pedreira</LOCAL> (onde o <PESSOA>Presidente da República</PESSOA> tem residência), <LOCAL>Santos-o-Velho</LOCAL> (do primeiro-ministro e de <PESSOA>Paulo Portas</PESSOA>) e Coração de Jesus (de <PESSOA>Francisco Louçã</PESSOA>), as três<LOCAL> Lisboa</LOCAL>, <PESSOA>Conceição</PESSOA>, na <LOCAL>Covilhã</LOCAL> (de <PESSOA>José Sócrates</PESSOA>) e <LOCAL>Santa Iria da Azóia</LOCAL>, em <LOCAL>Loures</LOCAL> (<PESSOA>Jerónimo de Sousa</PESSOA>)";
	
	public static void main(String[] args) {
		
		/*
		String a = "Setúbal-FC Porto</PER> ( II ) 7-01943-44 <ORG>FC Porto-Estoril</ORG>";
		String b = ", coordenadora da parte norte-americana do programa, e <PER>Charles L. Cooney</PER>, do <LOC>Centro Deshpande para a Inovação Tecnológica</LOC> -- e de docentes e empresas portuguesas que trabalham em articulação com a investigação universitária.";
		cleanBegin(b);
		*/

		String text = "and PC-3 for GE-90 engines";		
		text  = text.replaceAll("<[^>]+>[^<]+</?[^>]+>"," ").replaceAll("^[0-9]+?(,|\\.|/)?([0-9]+)?.?(º|ª|%)?", "");		
		System.out.println(text);
				
				
		
		/*
		//String t = "Do fim do crédito bonificado à habitação - \" medida socialmente errada e economicamente absurda \" - ao novo Código do Trabalho e ao desafio a <PESSOA>Durão Barroso</PESSOA> para que desça o IVA novamente para 17 por cento no Orçamento de Estado para 2003, passando pelo \" discurso pessimista \" sobre a situação económica do país, <PESSOA>Ferro Rodrigues</PESSOA> apontou baterias à governação do Executivo PSD-CDS/PP.";	
		//String t = "O chefe de Estado nomeou ainda como vogais da <ORGANIZACAO>Comissão Pedro Canavarro</ORGANIZACAO>, <PESSOA>presidente da <ORGANIZACAO>Fundação</ORGANIZACAO> Passos Canavarro</PESSOA>, <PESSOA>Maria de Lurdes Asseiro Luz</PESSOA>, o <PESSOA>Coronel António Manuel Garcia Correia</PESSOA> e <PESSOA>Pedro Rapoula</PESSOA>, assessor do <PESSOA>Presidente da República</PESSOA> para a área da Cultura.";

		String t = "Grande Prémio do Júri</MSC> foi entregue a \" Halbe Treppe\" de <PER>Andreas Dresen</PER>, um dos quatro filmes alemães na competição para o <LOC>Urso de Ouro</LOC>, enquanto <PER>Géorgien Otar Iosseliani</PER>, fixado em <LOC>França</LOC> há mais de vinte anos, obteve o Prémio para melhor realizador pelo filme \" Lundi matin\".";
				
		//Pattern pattern1 = Pattern.compile("(>|\\s)"+"Ferro Rodrigues"+"(\\s|<)");
    	//Pattern pattern2 = Pattern.compile("(>|\\s)"+"PS"+"(\\s|<)");
    	
		Pattern pattern = Pattern.compile("<PER>([^ÁÉA-Z0-9 ][^<]+)</PER>");
		Matcher matcher = pattern.matcher(t);
				
		while (matcher.find()) {
			System.out.println("antes: " + t);
			t = t.replace("<PESSOA>"+matcher.group(1)+"</PESSOA>", " "+matcher.group(1)+" ");
			System.out.println("depois: " + t);
			System.out.println();
			System.out.println();
		}
		
		System.out.println(t);
		
		Pattern pattern1 = Pattern.compile("<PESSOA>"+"[^<]+"+"</PESSOA>");
		Pattern pattern2 = Pattern.compile("<PESSOA>"+"[^<]+"+"</PESSOA>");

    	Matcher matcher1 = pattern1.matcher(t);
    	Matcher matcher2 = pattern2.matcher(t);
		matcher1 = pattern1.matcher(t);
		matcher2 = pattern2.matcher(t);                          
		matcher1.find();
		matcher2.find();
		
		System.out.println("sentence: " + t);
		System.out.println();
		System.out.println("left: " + t.substring(0,matcher1.start()));		
		System.out.println("left_clean: " + t.substring(0,matcher1.start()).replaceAll("(<[^>]+>[^<]+</[^>]+>)*",""));
		System.out.println();
		System.out.println("middle: " + t.substring(matcher1.end(),matcher2.start()));			
		System.out.println("middle_clean: " + t.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>",""));
		System.out.println();
		System.out.println("right: " + t.substring(matcher2.end()));
		System.out.println("right_clean: " + t.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>",""));
		System.out.println();
		System.out.println();
		*/
	}

	
	
	public static void cleanBegin(String text){
		text  = text.replaceAll("^[^<]+</[^>]+>"," ");
		System.out.println(text);
	}
}