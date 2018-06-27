package ciir.umass.edu.learning;

import ciir.umass.edu.utilities.Sorter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class RankList {
	protected List<DataPoint> rl = null;
	protected boolean hasclick = false;
	protected boolean clickIsMaxLabelAndSortedTop = false;

	public RankList() {
		this.rl = new ArrayList();
	}

	public RankList(RankList rl) {
		this.rl = new ArrayList();
		for (int i = 0; i < rl.size(); i++)
			this.rl.add(rl.get(i));
	}

	public RankList(RankList rl, int[] idx) {
		this.rl = new ArrayList();
		for (int i = 0; i < idx.length; i++)
			this.rl.add(rl.get(idx[i]));
	}

	public String getID() {
		return get(0).getID();
	}

	public int size() {
		return this.rl.size();
	}

	public DataPoint get(int k) {
		return (DataPoint) this.rl.get(k);
	}

	public boolean isClickIsMaxLabelAndSortedTop() {
		return this.clickIsMaxLabelAndSortedTop;
	}

	public void calculateSortedState(int k) {
		float maxLabel = -1.0F;
		float maxScore = -100.0F;
		int topDocIndex = -1;
		for (int i = 0; i < size(); i++) {
			if (maxLabel < get(i).getLabel()) {
				maxLabel = get(i).getLabel();
			}
			if (maxScore < get(i).martScore) {
				maxScore = get(i).martScore;
				topDocIndex = i;
			}
		}

		if ((get(topDocIndex)
				.getFeatureValue(((Integer) Node.featureidMap.get(Integer.valueOf(1230))).intValue()) == 1.0F)
				&& (Math.abs(maxLabel - get(topDocIndex).label) < 0.1D))
			this.clickIsMaxLabelAndSortedTop = true;
		else
			this.clickIsMaxLabelAndSortedTop = false;
	}

	public void setIsClick(boolean isClick) {
		this.hasclick = isClick;
	}

	public boolean isClick() {
		return this.hasclick;
	}

	public void add(DataPoint p) {
		this.rl.add(p);
	}

	public void remove(int k) {
		this.rl.remove(k);
	}

	public RankList getRanking(int fid) {
		double[] score = new double[this.rl.size()];
		for (int i = 0; i < this.rl.size(); i++)
			score[i] = ((DataPoint) this.rl.get(i)).getFeatureValue(fid);
		int[] idx = Sorter.sort(score, false);
		return new RankList(this, idx);
	}

	public RankList getCorrectRanking() {
		double[] score = new double[this.rl.size()];
		for (int i = 0; i < this.rl.size(); i++)
			score[i] = ((DataPoint) this.rl.get(i)).getLabel();
		int[] idx = Sorter.sort(score, false);
		return new RankList(this, idx);
	}

	public RankList getWorstRanking() {
		double[] score = new double[this.rl.size()];
		for (int i = 0; i < this.rl.size(); i++)
			score[i] = ((DataPoint) this.rl.get(i)).getLabel();
		int[] idx = Sorter.sort(score, true);
		return new RankList(this, idx);
	}

	public float getSplitDifficulty() {
		float maxLabel = 0.0F;
		for (int r = 0; r < this.rl.size(); r++) {
			if (maxLabel < ((DataPoint) this.rl.get(r)).getLabel()) {
				maxLabel = ((DataPoint) this.rl.get(r)).getLabel();
			}
		}
		double[] maxThresholds = new double[((DataPoint) this.rl.get(0)).getFeatureCount()];
		int[] splitDiff = new int[((DataPoint) this.rl.get(0)).getFeatureCount()];
		Arrays.fill(maxThresholds, -100000.0D);
		Arrays.fill(splitDiff, 1);

		for (int r = 0; r < this.rl.size(); r++) {
			((DataPoint) this.rl.get(r)).getLabel();
			if (Math.abs(maxLabel - ((DataPoint) this.rl.get(r)).getLabel()) < 0.1D) {
				for (int i = 0; i < maxThresholds.length; i++) {
					if (Node.idfeaturetotalMap.get(Integer.valueOf(i)) == null) {
						splitDiff[i] = 0;
					} else if (maxThresholds[i] < ((DataPoint) this.rl.get(r)).getFeatureValue(i)) {
						maxThresholds[i] = ((DataPoint) this.rl.get(r)).getFeatureValue(i);
					}
				}
			}
		}

		for (int r = 0; r < this.rl.size(); r++) {
			((DataPoint) this.rl.get(r)).getLabel();
			if (Math.abs(maxLabel - ((DataPoint) this.rl.get(r)).getLabel()) > 0.1D) {
				for (int i = 0; i < maxThresholds.length; i++) {
					if (Node.idfeaturetotalMap.get(Integer.valueOf(i)) == null) {
						continue;
					}
					if (maxThresholds[i] <= ((DataPoint) this.rl.get(r)).getFeatureValue(i)) {
						splitDiff[i] = 0;
					}
				}
			}
		}
		double res = 0.0D;
		for (int i = 0; i < splitDiff.length; i++) {
			res += splitDiff[i];
		}

		res *= maxLabel;
		return (float) res;
	}

	public float getSortQueryScore() {
		float[] maxScore = new float[6];
		Arrays.fill(maxScore, -100000.0F);
		int label = 0;
		int maxLabel = 0;
		float ms = -99999.0F;
		for (int i = 0; i < this.rl.size(); i++) {
			label = (int) ((DataPoint) this.rl.get(i)).getLabel();
			if (maxLabel < label) {
				maxLabel = label;
			}

			if (maxScore[label] < ((DataPoint) this.rl.get(i)).getMartScore()) {
				maxScore[label] = ((DataPoint) this.rl.get(i)).getMartScore();
			}
			if (ms < maxScore[label]) {
				ms = maxScore[label];
			}
		}
		float currentMax = -99999.0F;
		float currentMin = 99999.0F;
		float res = 0.0F;
		int hasLabel = 0;
		for (int i = maxScore.length - 1; i >= 0; i--) {
			if (maxScore[i] < -99999.0F) {
				continue;
			}
			hasLabel++;
			if (currentMin < maxScore[i])
				res += 2.0F;
			else if (currentMin == maxScore[i])
				res += 1.0F;
			else {
				currentMin = maxScore[i];
			}
		}
		res *= 5.0F;
		if (hasLabel == 1)
			hasLabel = 2;
		res /= (hasLabel - 1);

		if (maxScore[maxLabel] < ms) {
			res += 10.0F;
		}
		return res;
	}

	public void calculateScoreClass() {
		for (int i = 0; i < this.rl.size(); i++) {
			int scoreClass = 0;

			if (((DataPoint) this.rl.get(i)).getLabel() > 1.0F)
				scoreClass = 1;
			else {
				scoreClass = 2;
			}

			for (int j = 0; j < this.rl.size(); j++) {
				if ((((DataPoint) this.rl.get(j)).getLabel() < ((DataPoint) this.rl.get(i)).getLabel())
						&& (((DataPoint) this.rl.get(j)).getMartScore() > get(i).getMartScore())) {
					scoreClass = 0;
				}

				if ((((DataPoint) this.rl.get(j)).getLabel() <= ((DataPoint) this.rl.get(i)).getLabel())
						|| (((DataPoint) this.rl.get(j)).getMartScore() >= get(i).getMartScore()))
					continue;
				scoreClass = 0;
			}

			((DataPoint) this.rl.get(i)).setScoreClass(scoreClass);
		}
	}
}