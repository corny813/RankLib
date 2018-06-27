package ciir.umass.edu.learning.neuralnet;

public class PropParameter {
	public int current = -1;
	public int[][] pairMap = (int[][]) null;

	public float[][] pairWeight = (float[][]) null;
	public float[][] targetValue = (float[][]) null;

	public float[] labels = null;

	public PropParameter(int current, int[][] pairMap) {
		this.current = current;
		this.pairMap = pairMap;
	}

	public PropParameter(int current, int[][] pairMap, float[][] pairWeight, float[][] targetValue) {
		this.current = current;
		this.pairMap = pairMap;
		this.pairWeight = pairWeight;
		this.targetValue = targetValue;
	}

	public PropParameter(float[] labels) {
		this.labels = labels;
	}
}