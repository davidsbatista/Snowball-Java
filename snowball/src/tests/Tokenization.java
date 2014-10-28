package tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nlp.PortugueseTokenizer;
import bin.SnowballConfig;

public class Tokenization {
		
	public static void main(String[] args) {
		/*
		String text = "<MSC>Grande Prémio do Júri</MSC> foi entregue a \" Halbe Treppe\" de <PER>Andreas Dresen</PER>, um dos quatro filmes alemães na competição para o <LOC>Urso de Ouro</LOC>, enquanto <PER>Géorgien Otar Iosseliani</PER>, fixado em <LOC>França</LOC> há mais de vinte anos, obteve o Prémio para melhor realizador pelo filme \" Lundi matin\".";
		String sentence = ", coordenadora da parte norte-americana do programa, e <PER>Charles L. Cooney</PER>, do <LOC>Centro Deshpande para a Inovação Tecnológica</LOC> -- e de docentes e empresas portuguesas que trabalham em articulação com a investigação universitária.";
		String a = "Sob o lema \" Floresta: preservação e inovação\", <PER>Cavaco Silva</PER> visita na tarde de hoje uma empresa de exploração de Madeiras em <LOC>Oleiros</LOC> ( PINORVAL ), uma <MSC>Central Termoelétrica a Biomassa Florestal</MSC> na <LOC>Sertã</LOC> ( PALSER ) e, já em <LOC>Proença-a-Nova</LOC>, um <LOC>Centro de Ciência Viva da Floresta</LOC>.";
		String b = "e de <PER>General Torres</PER>, enquanto na <LOC>Linha Vermelha</LOC> as composições não circulamentre <LOC>Varziela</LOC> e <LOC>Vilar do Pinheiro</LOC>.";
		String c = "A <ORG>OGMA</ORG> - Indústria Aeronáutica de Portugal e a unidade na <LOC>China</LOC> ( a parceria Harbin-Embraer ) não estão incluídas porque a <ORG>Embraer</ORG> não detém 100 por cento do capital '', avançou o porta-voz.";
		splitSentence(c);
		*/
		tokenize("Zzuspicion that he forcefully disrupted business operations with the e-mail , '' said a police official of the   .");
	}
	
	public static void tokenize(String text){				
		text  = text.replaceAll("<[^>]+>[^<]+</?[^>]+>"," ").replaceAll("[0-9]+?(,|\\.|/)?([0-9]+)?.?(º|ª|%)?", "");
		/* tokenize  */
		
		List<String> terms = new ArrayList<String>();
		SnowballConfig.PTtokenizer = new PortugueseTokenizer();
		
		System.out.println(text);
		terms = (List<String>) Arrays.asList(text.split("\\s+"));
				
	}
	
	public static void splitSentence(String sentence){
		
		String e1_begin = "<ORG>";
		String e1_end = "</ORG>";
		String e2_begin = "<LOC>";
		String e2_end = "</LOC>";		
		Pattern pattern1 = Pattern.compile(e1_begin+"[^<]+"+e1_end);
		Pattern pattern2 = Pattern.compile(e2_begin+"[^<]+"+e2_end);		
		Matcher matcher1 = pattern1.matcher(sentence);
		Matcher matcher2 = pattern2.matcher(sentence);		
		matcher1.find();
		matcher2.find();
		
		// constructs vectors considering only tokens outside tags               		
		String left_txt = sentence.substring(0,matcher1.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
		String middle_txt = sentence.substring(matcher1.end(),matcher2.start()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
		String right_txt = sentence.substring(matcher2.end()).replaceAll("<[^>]+>[^<]+</[^>]+>","");
		
		System.out.println("\nleft_txt: ");
		System.out.println(left_txt);
		tokenize(left_txt);
		System.out.println("\nmiddle_txt: ");
		System.out.println(middle_txt);
		tokenize(middle_txt);		
		System.out.println("\nright_txt: ");
		System.out.println(right_txt);
		tokenize(right_txt);		
	}

}
