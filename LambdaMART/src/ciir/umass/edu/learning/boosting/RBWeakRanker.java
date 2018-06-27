package ciir.umass.edu.learning.boosting;

import ciir.umass.edu.learning.DataPoint;

public class RBWeakRanker {
	private int fid = -1;
	private double threshold = 0.0D;

	public RBWeakRanker(int fid, double threshold) {
		this.fid = fid;
		this.threshold = threshold;
	}

	public int score(DataPoint p) {
		if (p.getFeatureValue(this.fid) > this.threshold)
			return 1;
		return 0;
	}

	public int getFid() {
		return this.fid;
	}

	public double getThreshold() {
		return this.threshold;
	}

	public String toString() {
		return this.fid + ":" + this.threshold;
	}
}