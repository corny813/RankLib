package ciir.umass.edu.learning.neuralnet;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.MetricScorer;
import java.util.List;

public class LambdaRank extends RankNet {
	protected float[][] targetValue = (float[][]) null;

	public LambdaRank() {
	}

	public LambdaRank(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	protected int[][] batchFeedForward(RankList rl) {
		int[][] pairMap = new int[rl.size()][];
		this.targetValue = new float[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			addInput(rl.get(i));
			propagate(i);

			int count = 0;
			for (int j = 0; j < rl.size(); j++) {
				if ((rl.get(i).getLabel() > rl.get(j).getLabel()) || (rl.get(i).getLabel() < rl.get(j).getLabel()))
					count++;
			}
			pairMap[i] = new int[count];
			this.targetValue[i] = new float[count];

			int k = 0;
			for (int j = 0; j < rl.size(); j++) {
				if ((rl.get(i).getLabel() <= rl.get(j).getLabel()) && (rl.get(i).getLabel() >= rl.get(j).getLabel()))
					continue;
				pairMap[i][k] = j;
				if (rl.get(i).getLabel() > rl.get(j).getLabel())
					this.targetValue[i][k] = 1.0F;
				else
					this.targetValue[i][k] = 0.0F;
				k++;
			}
		}
		return pairMap;
	}

	protected void batchBackPropagate(int[][] pairMap, float[][] pairWeight) {
		for (int i = 0; i < pairMap.length; i++) {
			PropParameter p = new PropParameter(i, pairMap, pairWeight, this.targetValue);

			this.outputLayer.computeDelta(p);
			for (int j = this.layers.size() - 2; j >= 1; j--) {
				((Layer) this.layers.get(j)).updateDelta(p);
			}

			this.outputLayer.updateWeight(p);
			for (int j = this.layers.size() - 2; j >= 1; j--)
				((Layer) this.layers.get(j)).updateWeight(p);
		}
	}

	protected RankList internalReorder(RankList rl) {
		return rank(rl);
	}

	protected float[][] computePairWeight(int[][] pairMap, RankList rl) {
		double[][] changes = this.scorer.swapChange(rl);
		float[][] weight = new float[pairMap.length][];
		for (int i = 0; i < weight.length; i++) {
			weight[i] = new float[pairMap[i].length];
			for (int j = 0; j < pairMap[i].length; j++) {
				int sign = rl.get(i).getLabel() > rl.get(pairMap[i][j]).getLabel() ? 1 : -1;
				weight[i][j] = ((float) Math.abs(changes[i][pairMap[i][j]]) * sign);
			}
		}
		return weight;
	}

	protected void estimateLoss() {
		this.misorderedPairs = 0;
		for (int j = 0; j < this.samples.size(); j++) {
			RankList rl = (RankList) this.samples.get(j);
			for (int k = 0; k < rl.size() - 1; k++) {
				double o1 = eval(rl.get(k));
				for (int l = k + 1; l < rl.size(); l++) {
					if (rl.get(k).getLabel() <= rl.get(l).getLabel())
						continue;
					double o2 = eval(rl.get(l));

					if (o1 < o2) {
						this.misorderedPairs += 1;
					}
				}
			}
		}
		this.error = (1.0D - this.scoreOnTrainingData);
		if (this.error > this.lastError) {
			this.straightLoss += 1;
		} else
			this.straightLoss = 0;
		this.lastError = this.error;
	}

	public Ranker clone() {
		return new LambdaRank();
	}

	public String name() {
		return "LambdaRank";
	}
}