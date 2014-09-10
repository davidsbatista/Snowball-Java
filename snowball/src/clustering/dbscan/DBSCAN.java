package clustering.dbscan;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import vsm.TermsVector;

public class DBSCAN {
	
	public class CosineMeasure implements DistanceMeasure{

		private static final long serialVersionUID = 1L;

		@Override
		public double compute(double[] arg0, double[] arg1) {
			TermsVector.cosSimilarity(arg0,arg1);
			return 0;
		}		
	}
	
	/*
		eps: the distance that defines the ε-neighborhood of a point
		minPoints: the minimum number of density-connected points required to form a cluster
	*/
	
	DBSCANClusterer<Clusterable> cluster = new DBSCANClusterer<>(0, 0);
	

}
