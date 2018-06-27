package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.SimpleMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DCGScorer extends MetricScorer {

	protected static double[] discount = null;// cache
	protected static double[] gain = null;// cache
	public static double basiclog = 1.1D;

	public DCGScorer() {
		this.k = 10;
		if (discount == null) {
			discount = new double[5000];
			for (int i = 0; i < discount.length; i++)
				discount[i] = 1.0 / SimpleMath.logBase2(i + 2);
			gain = new double[6];
			for (int i = 0; i < 6; i++)
				gain[i] = (1 << i) - 1;// 2^i-1
		}
	}

	public DCGScorer(int k) {
		this.k = k;
		if (discount == null) {
			discount = new double[5000];
			for (int i = 0; i < discount.length; i++)
				discount[i] = 1.0 / SimpleMath.logBase2(i + 2);
			gain = new double[6];
			for (int i = 0; i < 6; i++)
				gain[i] = (1 << i) - 1;// 2^i - 1
		}
	}

	public MetricScorer clone() {
		return new DCGScorer();
	}

	public double score(RankList rl) {
		List<Integer> rel = new ArrayList<>();
		for (int i = 0; i < rl.size(); i++)
			rel.add(Integer.valueOf((int) rl.get(i).getLabel()));
		if (rl.size() < 1) {
			return -1.0D;
		}
		return getDCG(rel, this.k);
	}

	public String name() {
		return "DCG@" + this.k;
	}

	private double getDCG(List<Integer> rel, int k) {
		int size = k;
		if ((k > rel.size()) || (k <= 0)) {
			size = rel.size();
		}

		double dcg = 0.0D;
		for (int i = 1; i <= size; i++) {
			dcg += (Math.pow(2.0D, ((Integer) rel.get(i - 1)).intValue()) - 1.0D) / SimpleMath.logBase2(i + 1);
		}
		return dcg;
	}

	protected double getDCG(int[] rel, int topK) {
		double dcg = 0;
		
		for(int i = 0; i < topK; i++) {
			dcg += gain(rel[i]) * discount(i);
		}
		return dcg;
	}
	
	public double[][] swapChange(RankList rl) {
		int[] rel = getRelevanceLabels(rl);
		int size = (rl.size() > k) ? k : rl.size();
		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++)
			changes[i] = new double[rl.size()];

		for (int i = 0; i < size; i++) {
			for (int j = i + 1; j < rl.size(); j++) {
				changes[j][i] = changes[i][j] = (discount(i) - discount(j)) * (gain(rel[i]) - gain(rel[j]));
			}
		}
		return changes;
	}

//	public int[] getRelevanceLabels(RankList rl) {
//		int[] rel = new int[rl.size()];
//		for (int i = 0; i < rl.size(); i++)
//			rel[i] = (int) rl.get(i).getLabel();
//		return rel;
//	}

	protected double gain(int rel) {
		if (rel < gain.length)
			return gain[rel];

		// we need to expand our cache
		int cacheSize = gain.length + 10;
		while (cacheSize <= rel)
			cacheSize += 10;
		double[] tmp = new double[cacheSize];
		System.arraycopy(gain, 0, tmp, 0, gain.length);
		for (int i = gain.length; i < tmp.length; i++)
			tmp[i] = (1 << i) - 1;// 2^i - 1
		gain = tmp;
		return gain[rel];
	}

	// lazy caching
	protected double discount(int index) {
		if (index < discount.length)
			return discount[index];

		// we need to expand our cache
		int cacheSize = discount.length + 1000;
		while (cacheSize <= index)
			cacheSize += 1000;
		double[] tmp = new double[cacheSize];
		System.arraycopy(discount, 0, tmp, 0, discount.length);
		for (int i = discount.length; i < tmp.length; i++)
			tmp[i] = 1.0 / SimpleMath.logBase2(i + 2);
		discount = tmp;
		return discount[index];
	}

	public double[][] swapChange2(RankList rl) {
		int size = rl.size() > this.k ? this.k : rl.size();
		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0.0D);
		}

		for (int i = 0; i < size; i++) {
			int p1 = i + 1;
			for (int j = 0; j < rl.size(); j++) {
				if (i != j) {
					int p2 = j + 1;

					double tmpChange = (1.0D / SimpleMath.logBase2(p1 + 1) - 1.0D / SimpleMath.logBase2(p2 + 1))
							* (Math.pow(2.0D, rl.get(i).getLabel()) - Math.pow(2.0D, rl.get(j).getLabel()));
					if (i < j) {
						changes[i][j] = (tmpChange / Math.pow(basiclog, j - 1));
						changes[j][i] = (tmpChange / Math.pow(basiclog, i));
					} else {
						changes[i][j] = (tmpChange / Math.pow(basiclog, j));
						changes[j][i] = (tmpChange / Math.pow(basiclog, i - 1));
					}
				}

			}

		}

		return changes;
	}

	public double[][] swapChange3(RankList rl) {
		int size = rl.size() > this.k ? this.k : rl.size();
		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
		}
		float maxLabel = 0.0F;
		for (int i = 0; i < rl.size(); i++) {
			if (maxLabel < rl.get(i).getLabel()) {
				maxLabel = rl.get(i).getLabel();
			}
		}
		float changeLabel = maxLabel;
		if (rl.get(0).getLabel() < maxLabel - 0.1D) {
			changeLabel = 5.0F;
		}
		changeLabel = 5.0F;

		for (int i = 0; i < size; i++) {
			int p1 = i + 1;
			for (int j = i + 1; j < rl.size(); j++) {
				int p2 = j + 1;
				float label_i = rl.get(i).getLabel() > maxLabel - 0.1D ? changeLabel : rl.get(i).getLabel();
				float label_j = rl.get(j).getLabel() > maxLabel - 0.1D ? changeLabel : rl.get(j).getLabel();
				double tmp301_300 = ((1.0D / SimpleMath.logBase2(p1 + 1) - 1.0D / SimpleMath.logBase2(p2 + 1))
						* (Math.pow(2.0D, label_i) - Math.pow(2.0D, label_j)));
				changes[i][j] = tmp301_300;
				changes[j][i] = tmp301_300;
			}
		}
		return changes;
	}
}