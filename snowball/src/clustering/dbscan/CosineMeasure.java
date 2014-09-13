package clustering.dbscan;

import org.apache.commons.math3.ml.distance.DistanceMeasure;

public class CosineMeasure implements DistanceMeasure{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public static double norm(double[] a){
		double norm = 0;
		for (int i = 0; i < a.length; i++) {
			norm += Math.pow(a[i], 2);
		}						
		return Math.sqrt(norm);
	}
	
	public static double dotProdut(double[] a, double[] b) {		
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum = sum + (a[i] * b[i]);
		}	
		return sum;
	} 

	@Override
	public double compute(double[] arg0, double[] arg1) {
		return (double) 1-(dotProdut(arg0,arg1) / (norm(arg0) * norm(arg1)));
	} 
}