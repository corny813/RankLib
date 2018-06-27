package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class FeatureHistogram {
	public static float samplingRate = 1.0F;
	public static int notuseFeature = -1;

	public int[] features = null;
	public float[][] thresholds = (float[][]) null;
	public double[][] sum = (double[][]) null;
	public double[][] sqSum = (double[][]) null;
	public int[][] count = (int[][]) null;
	public int[][] sampleToThresholdMap = (int[][]) null;
	public Set<Integer> fSet;
	public double[][] sumQuery = (double[][]) null;
	public DataPoint[] allSamples = null;

	int totalCount = 312518;
	public int LambdaMartFeatureId = -1;

	public FeatureHistogram() {
		this.fSet = new HashSet(100);
		int[] fArray = { 1121, 1200, 1041, 1021, 1101, 1081, 1000, 1061, 1162, 1152 };
		for (int i : fArray)
			this.fSet.add(Integer.valueOf(i));
	}

	public void construct(DataPoint[] samples, float[] labels, int[][] sampleSortedIdx, int[] features,
			float[][] thresholds) {
		this.features = features;
		this.thresholds = thresholds;
		this.allSamples = samples;

		this.sum = new double[features.length][];
		this.sqSum = new double[features.length][];
		this.count = new int[features.length][];
		this.sampleToThresholdMap = new int[features.length][];
		int tmpMin = 0;
		int tmpMax = 0;

		for (int i = 0; i < features.length; i++) {
			int fid = features[i];

			int[] idx = sampleSortedIdx[i];

			double sumLeft = 0.0D;
			double sqSumLeft = 0.0D;
			float[] threshold = thresholds[i];
			double[] sumLabel = new double[threshold.length];
			double[] sqSumLabel = new double[threshold.length];
			int[] c = new int[threshold.length];
			int[] stMap = new int[samples.length];

			int last = -1;

			for (int t = 0; t < threshold.length; t++) {
				int j = last + 1;

				for (; j < idx.length; j++) {
					int k = idx[j];
					if (samples[k].getFeatureValue(fid) > threshold[t])
						break;
					sumLeft += labels[k];
					sqSumLeft += labels[k] * labels[k];
					stMap[k] = t;
				}

				last = j - 1;
				sumLabel[t] = sumLeft;
				sqSumLabel[t] = sqSumLeft;
				c[t] = (last + 1);
			}
			this.sampleToThresholdMap[i] = stMap;
			this.sum[i] = sumLabel;
			this.sqSum[i] = sqSumLabel;
			this.count[i] = c;
		}

		int[] useSamples = new int[samples.length];
		for (int i = 0; i < samples.length; i++) {
			useSamples[i] = i;
		}

		updateSplitQuery(useSamples);
	}

	public void updateLambdaFeature(DataPoint[] samples, float[] labels, int[][] sampleSortedIdx, int[] features,
			float[][] thresholds, int LambdaMartFeatureId) {
		this.features = features;
		this.thresholds = thresholds;
		this.allSamples = samples;
		this.LambdaMartFeatureId = LambdaMartFeatureId;

		for (int i = 0; i < features.length; i++) {
			if (i != LambdaMartFeatureId) {
				continue;
			}
			int fid = features[i];

			int[] idx = sampleSortedIdx[i];

			double sumLeft = 0.0D;
			double sqSumLeft = 0.0D;
			float[] threshold = thresholds[i];
			double[] sumLabel = new double[threshold.length];
			double[] sqSumLabel = new double[threshold.length];
			int[] c = new int[threshold.length];
			int[] stMap = new int[samples.length];

			int last = -1;
			for (int t = 0; t < threshold.length; t++) {
				int j = last + 1;

				for (; j < idx.length; j++) {
					int k = idx[j];
					if (samples[k].getFeatureValue(fid) > threshold[t])
						break;
					sumLeft += labels[k];
					sqSumLeft += labels[k] * labels[k];
					stMap[k] = t;
				}
				last = j - 1;
				sumLabel[t] = sumLeft;
				sqSumLabel[t] = sqSumLeft;
				c[t] = (last + 1);
			}
			this.sampleToThresholdMap[i] = stMap;
			this.sum[i] = sumLabel;
			this.sqSum[i] = sqSumLabel;
			this.count[i] = c;
		}

		int[] useSamples = new int[samples.length];
		for (int i = 0; i < samples.length; i++)
			useSamples[i] = i;
	}

	public void update(float[] labels) {
		for (int f = 0; f < this.features.length; f++) {
			Arrays.fill(this.sum[f], 0.0D);
			Arrays.fill(this.sqSum[f], 0.0D);
		}

		for (int k = 0; k < labels.length; k++) {
			for (int f = 0; f < this.features.length; f++) {
				int t = this.sampleToThresholdMap[f][k];
				this.sum[f][t] += labels[k];
				this.sqSum[f][t] += labels[k] * labels[k];
			}
		}

		for (int f = 0; f < this.features.length; f++)
			for (int t = 1; t < this.thresholds[f].length; t++) {
				this.sum[f][t] += this.sum[f][(t - 1)];
				this.sqSum[f][t] += this.sqSum[f][(t - 1)];
			}
	}

	public void construct(FeatureHistogram parent, int[] soi, float[] labels, int[] origin) {
		this.features = parent.features;
		this.thresholds = parent.thresholds;
		this.allSamples = parent.allSamples;

		this.sum = new double[this.features.length][];
		this.sqSum = new double[this.features.length][];
		this.count = new int[this.features.length][];
		this.sampleToThresholdMap = parent.sampleToThresholdMap;

		for (int i = 0; i < this.features.length; i++) {
			float[] threshold = this.thresholds[i];
			this.sum[i] = new double[threshold.length];
			this.sqSum[i] = new double[threshold.length];
			this.count[i] = new int[threshold.length];
			Arrays.fill(this.sum[i], 0.0D);
			Arrays.fill(this.sqSum[i], 0.0D);
			Arrays.fill(this.count[i], 0);
		}

		for (int i = 0; i < soi.length; i++) {
			int k = soi[i];
			for (int f = 0; f < this.features.length; f++) {
				int t = this.sampleToThresholdMap[f][k];
				this.sum[f][t] += labels[k];
				this.sqSum[f][t] += labels[k] * labels[k];
				this.count[f][t] += 1;
			}
		}

		for (int f = 0; f < this.features.length; f++)
			for (int t = 1; t < this.thresholds[f].length; t++) {
				this.sum[f][t] += this.sum[f][(t - 1)];
				this.sqSum[f][t] += this.sqSum[f][(t - 1)];
				this.count[f][t] += this.count[f][(t - 1)];
			}
	}

	public void construct(FeatureHistogram parent, FeatureHistogram leftSibling, int[] origin) {
		this.features = parent.features;
		this.thresholds = parent.thresholds;
		this.allSamples = parent.allSamples;

		this.sum = new double[this.features.length][];
		this.sqSum = new double[this.features.length][];
		this.count = new int[this.features.length][];
		this.sampleToThresholdMap = parent.sampleToThresholdMap;

		for (int i = 0; i < this.features.length; i++) {
			float[] threshold = this.thresholds[i];
			this.sum[i] = new double[threshold.length];
			this.sqSum[i] = new double[threshold.length];
			this.count[i] = new int[threshold.length];
		}

		for (int f = 0; f < this.features.length; f++) {
			float[] threshold = this.thresholds[f];
			this.sum[f] = new double[threshold.length];
			this.sqSum[f] = new double[threshold.length];
			this.count[f] = new int[threshold.length];
			for (int t = 0; t < threshold.length; t++) {
				parent.sum[f][t] -= leftSibling.sum[f][t];
				this.sqSum[f][t] += parent.sqSum[f][t] - leftSibling.sqSum[f][t];
				this.count[f][t] += parent.count[f][t] - leftSibling.count[f][t];
			}
		}
	}

	public void updateSplitQuery(int[] origin) {
		this.sumQuery = new double[this.features.length][];
		for (int f = 0; f < this.features.length; f++) {
			this.sumQuery[f] = new double[this.thresholds[f].length + 1];
			Arrays.fill(this.sumQuery[f], 0.0D);
		}

		int[] topLabelFeatureMaxThreshold = new int[this.features.length];
		int[] topLabelFeatureMinThreshold = new int[this.features.length];
		int[] maxFeatureThreshold = new int[this.features.length];
		Arrays.fill(topLabelFeatureMinThreshold, 2000);
		float tmpMaxLabel = -1.0F;
		for (int i = 0; i < origin.length; i++) {
			for (int f = 0; f < this.features.length; f++) {
				int t = this.sampleToThresholdMap[f][origin[i]];
				if (this.allSamples[i].getLabel() > tmpMaxLabel) {
					topLabelFeatureMinThreshold[f] = t;
					topLabelFeatureMaxThreshold[f] = t;
				} else if (this.allSamples[i].getLabel() >= tmpMaxLabel) {
					if (topLabelFeatureMaxThreshold[f] < t) {
						topLabelFeatureMaxThreshold[f] = t;
					}
					if (topLabelFeatureMinThreshold[f] > t) {
						topLabelFeatureMinThreshold[f] = t;
					}
				}

				if (tmpMaxLabel == -1.0F) {
					maxFeatureThreshold[f] = t;
				}
				if (maxFeatureThreshold[f] < t) {
					maxFeatureThreshold[f] = t;
				}
			}
			if (this.allSamples[i].getLabel() > tmpMaxLabel) {
				tmpMaxLabel = this.allSamples[i].getLabel();
			}
			if ((i == origin.length - 1)
					|| (!this.allSamples[origin[i]].getID().equals(this.allSamples[origin[(i + 1)]].getID()))) {
				for (int f = 0; f < this.features.length; f++) {
					this.sumQuery[f][0] += -1.0D;

					if (topLabelFeatureMinThreshold[f] == maxFeatureThreshold[f]) {
						this.sumQuery[f][topLabelFeatureMinThreshold[f]] += 2.0D;
					} else {
						this.sumQuery[f][topLabelFeatureMinThreshold[f]] += 1.0D;

						if (maxFeatureThreshold[f] + 1 < this.sumQuery[f].length) {
							this.sumQuery[f][(maxFeatureThreshold[f] + 1)] += 1.0D;
						}
					}
					this.sumQuery[f][(this.sumQuery[f].length - 1)] += 1.0D;
				}
				tmpMaxLabel = -1.0F;
				Arrays.fill(topLabelFeatureMaxThreshold, 0);
				Arrays.fill(topLabelFeatureMinThreshold, 2000);
				Arrays.fill(maxFeatureThreshold, 0);
			}
		}

		for (int f = 0; f < this.features.length; f++)
			for (int t = 1; t < this.sumQuery[f].length - 1; t++)
				this.sumQuery[f][t] += this.sumQuery[f][(t - 1)];
	}

	public Split findBestSplit(Split sp, DataPoint[] samples, float[] labels, int minLeafSupport) {
		if ((sp.getDeviance() >= 0.0D) && (sp.getDeviance() <= 0.0D)) {
			System.out.println("getDeviance is null " + sp.getDeviance());
			return null;
		}

		int bestFeatureIdx = -1;
		int bestThresholdIdx = -1;
		double bestVarLeft = -1.0D;
		double bestVarRight = -1.0D;
		double minS = 1.7976931348623157E+308D;

		int[] usedFeatures = null;
		if (samplingRate < 1.0F) {
			int size = (int) (samplingRate * this.features.length);
			usedFeatures = new int[size];

			List fpool = new ArrayList();
			for (int i = 0; i < this.features.length; i++) {
				fpool.add(Integer.valueOf(i));
			}
			Random r = new Random();
			for (int i = 0; i < size; i++) {
				int sel = r.nextInt(fpool.size());
				usedFeatures[i] = ((Integer) fpool.get(sel)).intValue();
				fpool.remove(sel);
			}
		} else {
			usedFeatures = new int[this.features.length];
			for (int i = 0; i < this.features.length; i++) {
				usedFeatures[i] = i;
			}
		}

		int continueCount = 0;
		int continueCount_ = 0;
		int unLockTree = 80;

		float splitRateBest = 0.0F;
		for (int f = 0; f < usedFeatures.length; f++) {
			int i = usedFeatures[f];
			if (this.features[i] != notuseFeature) {
				float[] threshold = this.thresholds[i];

				double[] sumLabel = this.sum[i];
				double[] sqSumLabel = this.sqSum[i];
				int[] sampleCount = this.count[i];

				double s = sumLabel[(sumLabel.length - 1)];
				double sq = sqSumLabel[(sumLabel.length - 1)];
				int c = sampleCount[(sumLabel.length - 1)];

				for (int t = 0; t < threshold.length; t++) {
					int countLeft = sampleCount[t];
					int countRight = c - countLeft;
					if ((countLeft < minLeafSupport) || (countRight < minLeafSupport)) {
						continueCount++;
					} else {
						continueCount_++;
						double sumLeft = sumLabel[t];
						double sqSumLeft = sqSumLabel[t];

						double sumRight = s - sumLeft;
						double sqSumRight = sq - sqSumLeft;

						double varLeft = sqSumLeft - sumLeft * sumLeft / countLeft;
						double varRight = sqSumRight - sumRight * sumRight / countRight;

						double S = varLeft + varRight;

						if (minS <= S) {
							continue;
						}
						minS = S;
						bestFeatureIdx = i;
						bestThresholdIdx = t;
						bestVarLeft = varLeft;
						bestVarRight = varRight;
					}
				}
			}
		}

		if (minS >= 1.7976931348623157E+308D) {
			return null;
		}

		float[] threshold = this.thresholds[bestFeatureIdx];
		double[] sumLabel = this.sum[bestFeatureIdx];
		int[] sampleCount = this.count[bestFeatureIdx];

		double s = sumLabel[(sumLabel.length - 1)];
		int c = sampleCount[(sumLabel.length - 1)];

		double sumLeft = sumLabel[bestThresholdIdx];
		int countLeft = sampleCount[bestThresholdIdx];

		double sumRight = s - sumLeft;
		int countRight = c - countLeft;

		int[] left = new int[countLeft];
		int[] right = new int[countRight];
		int[] originLeft = new int[countLeft];
		int[] originRight = new int[countRight];
		int l = 0;
		int r = 0;
		int[] idx = sp.getSamples();
		float leftLabel = 0.0F;
		float leftLabelsq = 0.0F;
		float rightLabel = 0.0F;
		float rightLabelsq = 0.0F;
		for (int j = 0; j < idx.length; j++) {
			int k = idx[j];
			if (samples[k].getFeatureValue(this.features[bestFeatureIdx]) <= threshold[bestThresholdIdx]) {
				left[(l++)] = k;

				originLeft[(l - 1)] = k;
				leftLabel += samples[k].getLabel();
				leftLabelsq += samples[k].getLabel() * samples[k].getLabel();
			} else {
				right[(r++)] = k;

				originRight[(r - 1)] = k;
				rightLabel += samples[k].getLabel();
				rightLabelsq += samples[k].getLabel() * samples[k].getLabel();
			}
		}

		FeatureHistogram lh = new FeatureHistogram();
		lh.construct(sp.hist, left, labels, originLeft);
		FeatureHistogram lr = new FeatureHistogram();
		lr.construct(sp.hist, lh, originRight);

		sp.set(this.features[bestFeatureIdx], this.thresholds[bestFeatureIdx][bestThresholdIdx], (float) minS,
				bestThresholdIdx);

		sp.setLeft(new Split(left, lh, (float) bestVarLeft, sumLeft, leftLabel, leftLabelsq, splitRateBest, -1,
				sp.treeDeep + 1, sp.getParentOutput()));
		sp.setRight(new Split(right, lr, (float) bestVarRight, sumRight, rightLabel, rightLabelsq, splitRateBest, 1,
				sp.treeDeep + 1, sp.getParentOutput()));

		return sp;
	}

	public Split setBestSplit(Split sp, DataPoint[] samples, float[] labels, int minLeafSupport, Split existSp) {
		int bestThresholdIdx = existSp.bestThresholdId;
		int bestFeatureIdx = 0;
		double bestVarLeft = -1.0D;
		double bestVarRight = -1.0D;
		double minS = 1.7976931348623157E+308D;
		float splitRateBest = 0.0F;
		for (int i = 0; i < this.features.length; i++) {
			if (this.features[i] == existSp.getFeatureID()) {
				bestFeatureIdx = i;
			}
		}
		float[] threshold = this.thresholds[bestFeatureIdx];
		double[] sumLabel = this.sum[bestFeatureIdx];
		double[] sqSumLabel = this.sqSum[bestFeatureIdx];
		int[] sampleCount = this.count[bestFeatureIdx];

		double s = sumLabel[(sumLabel.length - 1)];
		double sq = sqSumLabel[(sumLabel.length - 1)];
		int c = sampleCount[(sumLabel.length - 1)];

		double sumLeft = sumLabel[bestThresholdIdx];
		double sqSumLeft = sqSumLabel[bestThresholdIdx];
		int countLeft = sampleCount[bestThresholdIdx];

		double sumRight = s - sumLeft;
		double sqSumRight = sq - sqSumLeft;
		int countRight = c - countLeft;

		double originDeviance = sq - s * s / c;
		bestVarLeft = sqSumLeft - sumLeft * sumLeft / countLeft;
		bestVarRight = sqSumRight - sumRight * sumRight / countRight;
		minS = bestVarLeft + bestVarRight;

		int[] left = new int[countLeft];
		int[] right = new int[countRight];
		int[] originLeft = new int[countLeft];
		int[] originRight = new int[countRight];
		int l = 0;
		int r = 0;
		int[] idx = sp.getSamples();
		float leftLabel = 0.0F;
		float leftLabelsq = 0.0F;
		float rightLabel = 0.0F;
		float rightLabelsq = 0.0F;
		for (int j = 0; j < idx.length; j++) {
			int k = idx[j];
			if (samples[k].getFeatureValue(this.features[bestFeatureIdx]) <= threshold[bestThresholdIdx]) {
				left[(l++)] = k;
				originLeft[(l - 1)] = j;
				leftLabel += samples[k].getLabel();
				leftLabelsq += samples[k].getLabel() * samples[k].getLabel();
			} else {
				right[(r++)] = k;
				originRight[(r - 1)] = j;
				rightLabel += samples[k].getLabel();
				rightLabelsq += samples[k].getLabel() * samples[k].getLabel();
			}

		}

		FeatureHistogram lh = new FeatureHistogram();

		lh.construct(sp.hist, left, labels, originLeft);

		FeatureHistogram lr = new FeatureHistogram();
		lr.construct(sp.hist, lh, originRight);

		sp.setOriginDeviance((float) originDeviance);
		sp.set(this.features[bestFeatureIdx], this.thresholds[bestFeatureIdx][bestThresholdIdx], (float) minS,
				bestThresholdIdx);

		if (sp.getLeft() == null) {
			sp.setLeft(new Split(left, lh, (float) bestVarLeft, sumLeft, leftLabel, leftLabelsq, splitRateBest, -1,
					sp.treeDeep + 1, sp.getParentOutput()));
			System.out.println("left is null ---------------------------------------------");
		} else {
			sp.getLeft().set(left, lh, (float) bestVarLeft, sumLeft, sp.treeDeep + 1);
		}
		if (sp.getRight() == null) {
			sp.setRight(new Split(right, lr, (float) bestVarRight, sumRight, rightLabel, rightLabelsq, splitRateBest, 1,
					sp.treeDeep + 1, sp.getParentOutput()));
			System.out.println("right is null ---------------------------------------------");
		} else {
			sp.getRight().set(right, lr, (float) bestVarRight, sumRight, sp.treeDeep + 1);
		}
		return sp;
	}

	public Split excludeNoiseSample(Split sp, DataPoint[] samples, float[] labels, int[] noisePoint) {
		int countLeft = 0;
		for (int i = 0; i < noisePoint.length; i++) {
			countLeft += 1 - noisePoint[i];
		}
		int[] left = new int[countLeft];
		int[] idx = sp.getSamples();
		int[] originLeft = new int[countLeft];
		int l = 0;
		float leftLabel = 0.0F;
		float leftLabelsq = 0.0F;

		for (int j = 0; j < idx.length; j++) {
			int k = idx[j];
			if (noisePoint[k] == 0) {
				left[(l++)] = k;
				originLeft[(l - 1)] = k;
				leftLabel += samples[k].getLabel();
				leftLabelsq += samples[k].getLabel() * samples[k].getLabel();
			}
		}

		FeatureHistogram lh = new FeatureHistogram();

		lh.construct(sp.hist, left, labels, originLeft);
		sp.setLeft(new Split(left, lh, (1.0F / 1.0F), leftLabel, leftLabel, leftLabelsq, 0.0F, -1, sp.treeDeep + 1,
				sp.getOutput()));
		return sp.getLeft();
	}
}