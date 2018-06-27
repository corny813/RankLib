package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ERRScorer extends MetricScorer {
	public static double MAX = 32.0D;

	public ERRScorer() {
		this.k = 10;
	}

	public ERRScorer(int k) {
		this.k = k;
	}

	public ERRScorer clone() {
		return new ERRScorer();
	}

	public double score(RankList rl) {
		int size = this.k;
		if ((this.k > rl.size()) || (this.k <= 0)) {
			size = rl.size();
		}
		List rel = new ArrayList();
		for (int i = 0; i < rl.size(); i++) {
			rel.add(Integer.valueOf((int) rl.get(i).getLabel()));
		}
		double s = 0.0D;
		double p = 1.0D;
		for (int i = 1; i <= size; i++) {
			double R = R(((Integer) rel.get(i - 1)).intValue());
			s += p * R / i;
			p *= (1.0D - R);
		}
		return s;
	}

	public double scoreERR3(RankList rl) {
		int size = 3;
		if (3 > rl.size()) {
			size = rl.size();
		}

		List rel = RankListToIntList(rl);

		double s = 0.0D;
		double p = 1.0D;
		for (int i = 1; i <= size; i++) {
			int label = ((Integer) rel.get(i - 1)).intValue();
			if (label == -1)
				return -1.0D;
			double R = R(label);
			s += p * R / i;
			p *= (1.0D - R);
		}
		return s;
	}

	public double scoreWithNotLabel(RankList rl) {
		int size = this.k;
		if (this.k > rl.size()) {
			size = rl.size();
		}

		List rel = RankListToIntList(rl);

		double s = 0.0D;
		double p = 1.0D;
		for (int i = 1; i <= size; i++) {
			int label = ((Integer) rel.get(i - 1)).intValue();
			if (label == -1)
				label = 0;
			double R = R(label);
			s += p * R / i;
			p *= (1.0D - R);
		}
		return s;
	}

	public double scoreWithoutNotLabel(RankList rl) {
		int size = this.k;
		if (this.k > rl.size()) {
			size = rl.size();
		}

		List rel = RankListToIntList(rl);

		double s = 0.0D;
		double p = 1.0D;
		int count = 0;
		for (int i = 1; i <= rel.size(); i++) {
			int label = ((Integer) rel.get(i - 1)).intValue();
			if (label != -1) {
				if (count >= size) {
					break;
				}
				double R = R(label);
				s += p * R / (count + 1);
				p *= (1.0D - R);
				count++;
			}
		}
		return s;
	}

	private List<Integer> RankListToIntList(RankList rl) {
		List rel = new ArrayList();
		for (int i = 0; i < rl.size(); i++)
			rel.add(Integer.valueOf((int) rl.get(i).getLabel()));
		return rel;
	}

	public String name() {
		return "ERR@" + this.k;
	}

	private double R(int rel) {
		return (Math.pow(2.0D, rel) - 1.0D) / MAX;
	}

	public double[][] swapChange(RankList rl) {
		int size = rl.size() > this.k ? this.k : rl.size();
		int[] labels = new int[rl.size()];
		double[] R = new double[rl.size()];
		double[] np = new double[rl.size()];
		double p = 1.0D;

		for (int i = 0; i < size; i++) {
			labels[i] = (int) rl.get(i).getLabel();
			R[i] = R(labels[i]);
			np[i] = (p * (1.0D - R[i]));
			p *= np[i];
		}

		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0.0D);
		}

		for (int i = 0; i < size; i++) {
			double v1 = 1.0D / (i + 1) * (i == 0 ? 1.0D : np[(i - 1)]);
			double change = 0.0D;
			for (int j = i + 1; j < rl.size(); j++) {
				if (labels[i] == labels[j]) {
					change = 0.0D;
				} else {
					change = v1 * (R[j] - R[i]);
					p = (i == 0 ? 1.0D : np[(i - 1)]) * (R[i] - R[j]);
					for (int k = i + 1; k < j; k++) {
						change += p * R[k] / (1 + k);
						p *= (1.0D - R[k]);
					}
					change += (np[(j - 1)] * (1.0D - R[j]) * R[i] / (1.0D - R[i]) - np[(j - 1)] * R[j]) / (j + 1);
				}
				double tmp399_397 = change;
				changes[i][j] = tmp399_397;
				changes[j][i] = tmp399_397;
			}
		}
		return changes;
	}
}