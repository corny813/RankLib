package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExcludeSortedDoc {
	public int[] features = null;
	public float[][] thresholds = (float[][]) null;
	public double[][] total = (double[][]) null;
	public double[][] totalSmall = (double[][]) null;
	public double[][] totalLarge = (double[][]) null;

	public FeatureHistogram hist = null;
	public DataPoint[] samples = null;

	public void construct(DataPoint[] samples, int[][] sampleSortedIdx, int[] features, float[][] thresholds,
			FeatureHistogram hist) {
		this.features = features;
		this.thresholds = thresholds;
		this.total = new double[features.length][];
		this.totalSmall = new double[features.length][];
		this.totalLarge = new double[features.length][];

		this.hist = hist;
		this.samples = samples;

		for (int i = 0; i < features.length; i++) {
			int fid = features[i];
			int[] idx = sampleSortedIdx[i];
			float[] threshold = thresholds[i];
			double sumLeft = 0.0D;
			double sumLeftSmall = 0.0D;
			double sumLeftLarge = 0.0D;
			double[] totalSum = new double[threshold.length];
			double[] totalSumSmall = new double[threshold.length];
			double[] totalSumLarge = new double[threshold.length];

			int last = -1;
			for (int t = 0; t < threshold.length; t++) {
				int j = last + 1;
				for (; j < idx.length; j++) {
					int k = idx[j];
					if (samples[k].getFeatureValue(fid) > threshold[t])
						break;
					sumLeft += 1.0D;
					if (samples[k].getScoreClass() == 2) {
						sumLeftSmall += 1.0D;
					}
					if (samples[k].getScoreClass() == 1) {
						sumLeftLarge += 1.0D;
					}
				}

				last = j - 1;
				totalSum[t] = (last + 1);
				totalSumSmall[t] = sumLeftSmall;
				totalSumLarge[t] = sumLeftLarge;
			}

			this.total[i] = totalSum;
			this.totalSmall[i] = totalSumSmall;
			this.totalLarge[i] = totalSumLarge;
		}
	}

	public void update(DataPoint[] samples) {
		for (int f = 0; f < this.features.length; f++) {
			Arrays.fill(this.totalSmall[f], 0.0D);
			Arrays.fill(this.totalLarge[f], 0.0D);
		}

		for (int k = 0; k < samples.length; k++) {
			for (int f = 0; f < this.features.length; f++) {
				int t = this.hist.sampleToThresholdMap[f][k];
				if (samples[k].getScoreClass() == 1) {
					this.totalLarge[f][t] += 1.0D;
				}

				if (samples[k].getScoreClass() == 2) {
					this.totalSmall[f][t] += 1.0D;
				}
			}
		}

		for (int f = 0; f < this.features.length; f++)
			for (int t = 1; t < this.thresholds[f].length; t++) {
				this.totalSmall[f][t] += this.totalSmall[f][(t - 1)];
				this.totalLarge[f][t] += this.totalLarge[f][(t - 1)];
			}
	}

	public void construct(ExcludeSortedDoc parent, int[] soi) {
		this.features = parent.features;
		this.thresholds = parent.thresholds;
		this.samples = parent.samples;

		this.total = new double[this.features.length][];
		this.totalSmall = new double[this.features.length][];
		this.totalLarge = new double[this.features.length][];

		this.hist = parent.hist;

		for (int i = 0; i < this.features.length; i++) {
			float[] threshold = this.thresholds[i];
			this.total[i] = new double[threshold.length];
			this.totalSmall[i] = new double[threshold.length];
			this.totalLarge[i] = new double[threshold.length];
			Arrays.fill(this.total[i], 0.0D);
			Arrays.fill(this.totalSmall[i], 0.0D);
			Arrays.fill(this.totalLarge[i], 0.0D);
		}

		for (int i = 0; i < soi.length; i++) {
			int k = soi[i];

			for (int f = 0; f < this.features.length; f++) {
				int t = this.hist.sampleToThresholdMap[f][k];
				this.total[f][t] += 1.0D;
				if (this.samples[k].getScoreClass() == 1) {
					this.totalLarge[f][t] += 1.0D;
				}
				if (this.samples[k].getScoreClass() == 2) {
					this.totalSmall[f][t] += 1.0D;
				}
			}
		}

		for (int f = 0; f < this.features.length; f++)
			for (int t = 1; t < this.thresholds[f].length; t++) {
				this.total[f][t] += this.total[f][(t - 1)];
				this.totalSmall[f][t] += this.totalSmall[f][(t - 1)];
				this.totalLarge[f][t] += this.totalLarge[f][(t - 1)];
			}
	}

	public List<Split> findBestSplit(Split sp, DataPoint[] samples, float accordanceRate, int minLeafSupport, int tag) {
		List res = new ArrayList();
		int bestFeatureIdx = -1;
		int bestThresholdIdx = -1;
		int bestCount = 0;
		double minS = 4.9E-324D;
		int direct = 0;
		double[][] selectSum;
		if (tag == 0)
			selectSum = this.totalSmall;
		else {
			selectSum = this.totalLarge;
		}

		System.out.println("search find Best Split");
		for (int f = 0; f < this.features.length; f++) {
			int i = f;
			float[] threshold = this.thresholds[i];
			double[] totalSum = this.total[i];
			double[] totalSelect = selectSum[i];
			double c = totalSum[(totalSum.length - 1)];
			double ct = totalSelect[(totalSelect.length - 1)];
			for (int t = 0; t < threshold.length; t++) {
				double countLeft = totalSum[t];
				double countRight = c - countLeft;
				if ((countLeft < minLeafSupport) || (countRight < minLeafSupport)) {
					continue;
				}
				double countSelectLeft = totalSelect[t];
				double countSelectRight = ct - countSelectLeft;
				if ((countSelectLeft > countLeft * accordanceRate) && (countSelectLeft > minS)) {
					minS = countSelectLeft;
					direct = -1;
					bestFeatureIdx = f;
					bestThresholdIdx = t;
					bestCount = (int) countLeft;
				}
				if ((countSelectRight > countRight * accordanceRate) && (countSelectRight > minS)) {
					minS = countSelectRight;
					direct = 1;
					bestFeatureIdx = f;
					bestThresholdIdx = t;
					bestCount = (int) countRight;
				}
			}
		}

		if (direct == 0) {
			return res;
		}

		float[] threshold = this.thresholds[bestFeatureIdx];
		int[] childSamples = new int[bestCount];
		int[] idx = sp.getSamples();
		System.out.println(idx.length + " " + bestCount);
		int lr = 0;
		for (int j = 0; j < idx.length; j++) {
			int k = idx[j];
			if ((samples[k].getFeatureValue(this.features[bestFeatureIdx]) <= threshold[bestThresholdIdx])
					&& (direct == -1)) {
				childSamples[(lr++)] = k;
			} else {
				if ((samples[k].getFeatureValue(this.features[bestFeatureIdx]) <= threshold[bestThresholdIdx])
						|| (direct != 1))
					continue;
				childSamples[(lr++)] = k;
			}
		}
		ExcludeSortedDoc es = new ExcludeSortedDoc();

		es.construct(sp.excludeSortedDoc, childSamples);
		sp.set(this.features[bestFeatureIdx], this.thresholds[bestFeatureIdx][bestThresholdIdx], (float) minS,
				bestThresholdIdx);
		if (direct == -1) {
			sp.setLeft(new Split(childSamples, es, 0.0F, 0.0D, 0));
			sp.setRight(new Split(-2, 0.0F, 0.0F));
			res.add(sp.getLeft());
			res.add(sp.getRight());
		} else {
			sp.setLeft(new Split(-2, 0.0F, 0.0F));
			sp.setRight(new Split(childSamples, es, 0.0F, 0.0D, 0));
			res.add(sp.getRight());
			res.add(sp.getLeft());
		}

		return res;
	}

	public Split setChildNode(Split sp, DataPoint[] samples, int tag) {
		int sampleCnt = sp.getSamples().length;
		if (tag == 0) {
			Split child = sp.getLeft();
			sampleCnt -= sp.getRight().getSampleCnt();
		} else {
			Split child = sp.getRight();
			sampleCnt -= sp.getLeft().getSampleCnt();
		}
		int bestFeatureIdx = -1;
		for (int i = 0; i < this.features.length; i++) {
			if (this.features[i] == sp.getFeatureID()) {
				bestFeatureIdx = i;
			}
		}

		float[] threshold = this.thresholds[bestFeatureIdx];
		int[] childSamples = new int[sampleCnt];
		System.out.println(sampleCnt + " childSampleCnt");
		int[] idx = sp.getSamples();
		int lr = 0;
		for (int j = 0; j < idx.length; j++) {
			int k = idx[j];
			if ((samples[k].getFeatureValue(sp.getFeatureID()) <= threshold[sp.bestThresholdId]) && (tag == 0)) {
				childSamples[(lr++)] = k;
			} else {
				if ((samples[k].getFeatureValue(sp.getFeatureID()) <= threshold[sp.bestThresholdId]) || (tag != 1))
					continue;
				childSamples[(lr++)] = k;
			}
		}
		ExcludeSortedDoc es = new ExcludeSortedDoc();

		es.construct(sp.excludeSortedDoc, childSamples);
		return new Split(childSamples, es, 0.0F, 0.0D, 0);
	}
}