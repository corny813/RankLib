package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Split {
	private int featureID = -1;
	private float threshold = 0.0F;
	private float avgLabel = 0.0F;
	private float sumOriginLabel = 0.0F;
	private float sqSumOriginLabel = 0.0F;

	private double sumLabel = 0.0D;
	private double sqSumLabel = 0.0D;
	private Split left = null;
	private Split right = null;
	private float deviance = 0.0F;
	private float originDeviance = 0.0F;
	private int[][] sortedSampleIDs = (int[][]) null;
	public int[] samples = null;
	public FeatureHistogram hist = null;
	protected ExcludeSortedDoc excludeSortedDoc = null;
	public int sampleCnt = 0;
	public String outMess = null;
	public float splitRate = 0.0F;
	public int direct = 0;
	public int treeDeep = 0;
	public int bestThresholdId = -1;
	public float accordanceRate = -1.0F;
	public float parent_output = 0.0F;
	public int isSplit = 0;

	public Split() {
	}

	public Split(int featureID, float threshold, float deviance) {
		this.featureID = featureID;
		this.threshold = threshold;
		this.deviance = deviance;
		this.originDeviance = deviance;
	}

	public Split(int[][] sortedSampleIDs, float deviance, double sumLabel, double sqSumLabel) {
		this.sortedSampleIDs = sortedSampleIDs;
		this.deviance = deviance;
		this.sumLabel = sumLabel;
		this.sqSumLabel = sqSumLabel;
		this.avgLabel = (float) (sumLabel / sortedSampleIDs[0].length);
		this.originDeviance = deviance;
	}

	public Split(int[] samples, FeatureHistogram hist, float deviance, double sumLabel, double sumOriginLabel,
			double sqSumOriginLabel, float splitRate, int direct, int treeDeep, float parent_output) {
		this.samples = samples;
		this.hist = hist;
		this.deviance = deviance;
		this.sumLabel = sumLabel;
		this.originDeviance = deviance;
		this.avgLabel = (float) (sumLabel / samples.length);
		this.sumOriginLabel = (float) sumOriginLabel;
		this.sqSumOriginLabel = (float) sqSumOriginLabel;
		this.splitRate = splitRate;
		this.direct = direct;
		this.treeDeep = treeDeep;
		this.parent_output = parent_output;
	}

	public Split(int[] samples, FeatureHistogram hist, float deviance, double sumLabel, int treeDeep) {
		this.samples = samples;
		this.hist = hist;
		this.deviance = deviance;
		this.sumLabel = sumLabel;
		this.avgLabel = (float) (sumLabel / samples.length);
		this.originDeviance = deviance;
		this.treeDeep = treeDeep;
	}

	public Split(int[] samples, ExcludeSortedDoc excludeSortedDoc, float deviance, double sumLabel, int treeDeep) {
		this.samples = samples;
		this.excludeSortedDoc = excludeSortedDoc;
		this.deviance = deviance;
		this.sumLabel = sumLabel;
		this.avgLabel = (float) (sumLabel / samples.length);
		this.originDeviance = deviance;
		this.treeDeep = treeDeep;
	}

	public void set(int featureID, float threshold, float deviance, int bestThresholdId) {
		this.featureID = featureID;
		this.threshold = threshold;
		this.deviance = deviance;
		this.bestThresholdId = bestThresholdId;
	}

	public void set(int[] samples, FeatureHistogram hist, float deviance, double sumLabel, int treeDeep) {
		this.samples = samples;
		this.hist = hist;
		this.deviance = deviance;
		this.sumLabel = sumLabel;
		this.avgLabel = (float) (sumLabel / samples.length);
		this.originDeviance = deviance;
		this.treeDeep = treeDeep;
	}

	public void setFeatureID(int featureID) {
		this.featureID = featureID;
	}

	public void setLeft(Split s) {
		this.left = s;
	}

	public void setRight(Split s) {
		this.right = s;
	}

	public void setIsSplit(int state) {
		this.isSplit = state;
	}

	public void setOutput(float output) {
		this.avgLabel = (output + this.parent_output);
	}

	public void setParentOutput(float output) {
		this.parent_output = output;
	}

	public void setOriginDeviance(float originDeviance) {
		this.originDeviance = originDeviance;
	}

	public Split getLeft() {
		return this.left;
	}

	public Split getRight() {
		return this.right;
	}

	public void SetOutMessage(String s) {
		this.outMess = s;
	}

	public float getDeviance() {
		return this.deviance;
	}

	public float getOriginDeviance() {
		return this.originDeviance;
	}

	public float getDevRevenue() {
		if (this.featureID == -1) {
			return 0.0F;
		}
		return this.originDeviance - this.left.getDeviance() - this.right.getDeviance();
	}

	public int getFeatureID() {
		return this.featureID;
	}

	public float getOutput() {
		return this.avgLabel;
	}

	public int getIsSplit() {
		return this.isSplit;
	}

	public float getParentOutput() {
		return this.parent_output;
	}

	public float getSumOriginLabel() {
		return this.sumOriginLabel;
	}

	public float getSqSumOriginLabel() {
		return this.sqSumOriginLabel;
	}

	public List<Split> leaves() {
		List list = new ArrayList();
		leaves(list);
		return list;
	}

	public void updateUseFeatureRate(int[] useFeatureRate) {
		if (this.featureID == -1) {
			return;
		}
		useFeatureRate[this.featureID] += 1;
		useFeatureRate[0] += 1;
		this.left.updateUseFeatureRate(useFeatureRate);
		this.right.updateUseFeatureRate(useFeatureRate);
	}

	private void leaves(List<Split> leaves) {
		if (this.featureID <= -1) {
			leaves.add(this);
		} else {
			this.left.leaves(leaves);

			this.right.leaves(leaves);
		}
	}

	public float eval(DataPoint dp) {
		if (this.featureID == -1) {
			return this.avgLabel;
		}
		if (dp.getFeatureValue(this.featureID) <= this.threshold) {
			return this.left.eval(dp);
		}
		return this.right.eval(dp);
	}

	public String toString() {
		return toString("");
	}

	public float getThreshold() {
		return this.threshold;
	}

	public String toString(String indent) {
		String strOutput = indent + "<split>\n";
		strOutput = strOutput + getString(new StringBuilder().append(indent).append("\t").toString());
		strOutput = strOutput + indent + "</split>\n";
		return strOutput;
	}

	public float getAveSplitScore() {
		int len = 0;
		if (this.samples == null)
			len = this.sampleCnt;
		else {
			len = this.samples.length;
		}
		return (float) this.sumLabel / len;
	}

	public int getSampleCnt() {
		if (this.samples == null) {
			return this.sampleCnt;
		}
		return this.samples.length;
	}

	public String getString(String indent) {
		String strOutput = "";
		if (this.featureID == -1) {
			strOutput = strOutput + indent + "<output> " + this.avgLabel + " </output>\n";

			strOutput = strOutput + indent + "<variance> " + this.deviance + " </variance>\n";
			strOutput = strOutput + indent + "<instCnt> " + this.sampleCnt + " </instCnt>\n";
		} else {
			int featureIDNew = 0;
			featureIDNew = ((Integer) Node.idfeatureMap.get(Integer.valueOf(this.featureID))).intValue();
			strOutput = strOutput + indent + "<feature> " + featureIDNew + " </feature>\n";
			strOutput = strOutput + indent + "<threshold> " + this.threshold + " </threshold>\n";

			strOutput = strOutput + indent + "<variance> " + this.deviance + " </variance>\n";
			strOutput = strOutput + indent + "<instCnt> " + this.sampleCnt + " </instCnt>\n";

			strOutput = strOutput + indent + "<split pos=\"left\">\n";
			strOutput = strOutput + this.left.getString(new StringBuilder().append(indent).append("\t").toString());
			strOutput = strOutput + indent + "</split>\n";
			strOutput = strOutput + indent + "<split pos=\"right\">\n";
			strOutput = strOutput + this.right.getString(new StringBuilder().append(indent).append("\t").toString());
			strOutput = strOutput + indent + "</split>\n";
		}
		return strOutput;
	}

	public int[] getSamples() {
		if (this.sortedSampleIDs != null) {
			return this.sortedSampleIDs[0];
		}
		return this.samples;
	}

	public int[][] getSampleSortedIndex() {
		return this.sortedSampleIDs;
	}

	public double getSumLabel() {
		return this.sumLabel;
	}

	public double getSqSumLabel() {
		return this.sqSumLabel;
	}

	public void clearSamples() {
		if (this.samples != null) {
			this.sampleCnt = this.samples.length;
		}

		this.sortedSampleIDs = ((int[][]) null);
		this.samples = null;
		this.hist = null;
		this.excludeSortedDoc = null;
	}
}