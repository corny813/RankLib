package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.util.Arrays;

public class BestAtKScorer extends MetricScorer {
	public BestAtKScorer() {
		this.k = 10;
	}

	public BestAtKScorer(int k) {
		this.k = k;
	}

	public double score(RankList rl) {
		return rl.get(maxToK(rl, this.k - 1)).getLabel();
	}

	public MetricScorer clone() {
		return new BestAtKScorer();
	}

	public int maxToK(RankList rl, int k) {
		int size = k;
		if ((size < 0) || (size > rl.size() - 1)) {
			size = rl.size() - 1;
		}
		double max = -1.0D;
		int max_i = 0;
		for (int i = 0; i <= size; i++) {
			if (max >= rl.get(i).getLabel())
				continue;
			max = rl.get(i).getLabel();
			max_i = i;
		}

		return max_i;
	}

	public String name() {
		return "Best@" + this.k;
	}

	public double[][] swapChange(RankList rl) {
		int[] labels = new int[rl.size()];
		int[] best = new int[rl.size()];
		int max = -1;
		int maxVal = -1;
		int secondMaxVal = -1;
		int maxCount = 0;
		for (int i = 0; i < rl.size(); i++) {
			int v = (int) rl.get(i).getLabel();
			labels[i] = v;
			if (maxVal < v) {
				if (i < this.k) {
					secondMaxVal = maxVal;
					maxCount = 0;
				}
				maxVal = v;
				max = i;
			} else if ((maxVal == v) && (i < this.k)) {
				maxCount++;
			}
			best[i] = max;
		}
		if (secondMaxVal == -1) {
			secondMaxVal = 0;
		}
		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0.0D);
		}

		for (int i = 0; i < rl.size() - 1; i++) {
			for (int j = i + 1; j < rl.size(); j++) {
				double change = 0.0D;
				if ((j < this.k) || (i >= this.k))
					change = 0.0D;
				else if ((labels[i] == labels[j]) || (labels[j] == labels[best[(this.k - 1)]]))
					change = 0.0D;
				else if (labels[j] > labels[best[(this.k - 1)]])
					change = labels[j] - labels[best[i]];
				else if ((labels[i] < labels[best[(this.k - 1)]]) || (maxCount > 1))
					change = 0.0D;
				else
					change = maxVal - Math.max(secondMaxVal, labels[j]);
				double tmp361_359 = change;
				changes[j][i] = tmp361_359;
				changes[i][j] = tmp361_359;
			}
		}
		return changes;
	}
}