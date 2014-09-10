package clustering.dbscan;

import java.util.*;

import bin.Config;

import clustering.SnowballPattern;

import tuples.Tuple;

public class Dbscan
{
		static List<Tuple> visitedTuples;
		static List<Tuple> alreadyClusterMember;
		
		public static LinkedList<SnowballPattern> applyDbscan(LinkedList<Tuple> tuples, LinkedList<SnowballPattern> clustersList) {
			double threshold = Config.parameters.get("min_degree_match");
			int minPts = Math.round(Config.parameters.get("DBScan_min_points"));
			List<Tuple> neighbours = new LinkedList<Tuple>();
			visitedTuples = new LinkedList<Tuple>();
			alreadyClusterMember = new LinkedList<Tuple>();
			
			SnowballPattern c = null;
			for(int i = 0; i < tuples.size(); i++) {
				Tuple t = tuples.get(i);
				if(!isVisited(t)){
					visited(t);
					neighbours = getNeighbours(t, tuples, threshold);
					
					if (neighbours.size() >= minPts){
						c = new SnowballPattern(t);
						int indexNeighbours = 0;
						while(neighbours.size() > indexNeighbours){		
							Tuple v = neighbours.get(indexNeighbours);
							if(!isVisited(v)){
								visited(v);
								List<Tuple> neighbours2 = getNeighbours(v, tuples, threshold);
								if (neighbours2.size() >= minPts){
									neighbours = mergeNeighbours(neighbours, neighbours2);
								}
							}
							if(!isClusterMember(v)) {
								c.addTuple(v);
							}
							indexNeighbours++;
						}
						if(c != null)
							clustersList.add(c);
					}
				}
			}
			return clustersList;
		}
		
		public static void visited(Tuple t) {
			visitedTuples.add(t);
		}

		public static boolean isVisited(Tuple t) { 
				if (visitedTuples.contains(t))
					return true;
				else
					return false;
		}
		
		public static List<Tuple> getNeighbours(Tuple t, LinkedList<Tuple> possibleNeighbours, double threshold) {
			List<Tuple> neighbours = new LinkedList<Tuple>();

			for(Tuple pt : possibleNeighbours) {
				if(t.degreeMatchCosTFIDF(pt.left, pt.middle, pt.right) > threshold) {
					neighbours.add(pt);
				}
			}
			
			return neighbours;
		}
		
		public static List<Tuple> mergeNeighbours(List<Tuple> a,List<Tuple> b) {
			return a;
		}
		
		public static boolean isClusterMember(Tuple t) { 
			if (alreadyClusterMember.contains(t))
				return true;
			else
				return false;
	} 
}
		

			






				