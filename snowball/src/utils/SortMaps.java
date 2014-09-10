package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import tuples.Seed;

public class SortMaps {

	public static void main(String[] args) {
		//Create a hashtable
		Hashtable<String,Integer> myhash=new Hashtable<String,Integer>();
	
		//Put things in Hashtable
		myhash.put("AAB", 2);
		myhash.put("ABC", 2);
		myhash.put("AAX", 3);
		
		//Put keys and values in to an arraylist using entryset
		ArrayList myArrayList=new ArrayList(myhash.entrySet());
	
		//Sort the values based on values first and then keys.
		Collections.sort(myArrayList, new StringIntegerComparator());
	
		//Show sorted results
		Iterator itr=myArrayList.iterator();
		String key="";
		int value=0;
		int cnt=0;
		while(itr.hasNext()){	
			cnt++;
			Map.Entry e=(Map.Entry)itr.next();		
			key = (String)e.getKey();
			value = ((Integer)e.getValue()).intValue();		
			System.out.println(key+", "+value);
		}

	}

	public static class StringIntegerComparator implements Comparator {
		
		public int compare(Object obj1, Object obj2) {
			int result = 0;
			Map.Entry<Seed,Integer> e1 = (Map.Entry<Seed,Integer>) obj1;
			Map.Entry<Seed,Integer> e2 = (Map.Entry<Seed,Integer>) obj2;
			
			// Sort based on values.
			Integer value1 = (Integer) e1.getValue();
			Integer value2 = (Integer) e2.getValue();
			if (value1.compareTo(value2) == 0) {
				// Sort String in an alphabetical order
				Seed s1 = (Seed) e1.getKey();
				Seed s2 = (Seed) e2.getKey();
				result = s1.compareTo(s2);
			} else {
				// Sort values in a descending order
				result = value2.compareTo(value1);
			}
				return result;
		}
	}
	
	public static class StringDoubleComparator implements Comparator {
	
		public int compare(Object obj1, Object obj2) {
	
			int result = 0;
			Map.Entry<String,Double> e1 = (Map.Entry<String,Double>) obj1;
			Map.Entry<String,Double> e2 = (Map.Entry<String,Double>) obj2;
				
			// Sort based on values.
			Double value1 = (Double) e1.getValue();
			Double value2 = (Double) e2.getValue();
				
			if (value1.compareTo(value2) == 0) {
				// Sort String in an alphabetical order
				String word1 = (String) e1.getKey();
				String word2 = (String) e2.getKey();
				result = word1.compareToIgnoreCase(word2);
			} else {
				// Sort values in a descending order
				result = value2.compareTo(value1);
			}
			return result;
		}
	}
}
