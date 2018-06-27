package ciir.umass.edu.learning;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.TreeMap;

public class DataPoint {
	public static float INFINITY = -1000000.0F;
	public static int MAX_FEATURE = 51;
	public static int FEATURE_INCREASE = 10;

	public static int featureCount = 0;

	protected float label = 0.0F;
	protected String id = "";
	protected float[] fVals = null;
	protected String description = "";

	protected double cached = -1.0D;

	public boolean isMaxLabel = false;
	public float lambdaTest = 0.0F;
	public float martScore = 0.0F;
	public int scoreClass = 0;
	public int classType = 0;
	public boolean isClick = false;

	private String getKey(String pair) {
		return pair.substring(0, pair.indexOf(":"));
	}

	private String getValue(String pair) {
		return pair.substring(pair.lastIndexOf(":") + 1);
	}

	public DataPoint(String text) {
		LoadDataPoint(text);
	}

	public void LoadDataPoint(String text) {
		float[] fVals_ = new float[MAX_FEATURE];
		Arrays.fill(fVals_, INFINITY);
		int lastFeature = -1;
		try {
			int idx = text.indexOf("#");
			if (idx != -1) {
				this.description = new String(text.substring(idx));
				text = new String(text.substring(0, idx).trim());
			}
			String[] fsTmp = text.split(" ");
			String[] fs = new String[fsTmp.length];
			for (int i = 0; i < fs.length; i++) {
				fs[i] = new String(fsTmp[i]);
			}
			this.label = Float.parseFloat(fs[0]);
			this.id = getValue(fs[1]);
			String key = "";
			String val = "";

			for (int i = 1; i < fs.length; i++) {
				if (i == 1) {
					key = "1";
					val = "0";
				} else {
					key = getKey(fs[i]);
					val = getValue(fs[i]);
				}
				int f = Integer.parseInt(key);
				if (!Node.featureidMap.containsKey(Integer.valueOf(f)))
					continue;
				f = ((Integer) Node.featureidMap.get(Integer.valueOf(f))).intValue();

				if (f >= MAX_FEATURE) {
					while (f >= MAX_FEATURE)
						MAX_FEATURE += FEATURE_INCREASE;
					float[] tmp = new float[MAX_FEATURE];
					System.arraycopy(fVals_, 0, tmp, 0, fVals_.length);
					Arrays.fill(tmp, fVals_.length, MAX_FEATURE, INFINITY);
					fVals_ = tmp;
				}
				fVals_[f] = Float.parseFloat(val);
				if (f > featureCount)
					featureCount = f;
				if (f > lastFeature) {
					lastFeature = f;
				}
			}
			float[] tmp = new float[lastFeature + 1];
			System.arraycopy(fVals_, 0, tmp, 0, lastFeature + 1);
			fVals_ = tmp;
		} catch (Exception ex) {
			System.out.println("Error in DataPoint(text) constructor");
		}
		this.fVals = fVals_;
	}

	public void setScoreClass(int sc) {
		this.scoreClass = sc;
	}

	public int getScoreClass() {
		return this.scoreClass;
	}

	public void setClassType(int sct) {
		this.classType = sct;
	}

	public int getClassType() {
		return this.classType;
	}

	public String getID() {
		return this.id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public float getLabel() {
		return this.label;
	}

	public void compareMaxLabel(float MaxLabel) {
		if (this.label > MaxLabel - 0.1D)
			this.isMaxLabel = true;
		else
			this.isMaxLabel = false;
	}

	public void setLabel(float label) {
		this.label = label;
	}

	public float getFeatureValue(int fid) {
		if (fid >= this.fVals.length)
			return 0.0F;
		if (this.fVals[fid] < INFINITY + 1.0F)
			return 0.0F;
		return this.fVals[fid];
	}

	public void setFeatureValue(int fid, float fval) {
		this.fVals[fid] = fval;
	}

	public void setFeatureValueUseFeature(int feature, float fval) {
		this.fVals[((Integer) Node.featureidMap.get(Integer.valueOf(feature))).intValue()] = fval;
	}

	public int getFeatureCount() {
		return featureCount;
	}

	public float[] getFeatureVector(int[] featureID) {
		float[] fvector = new float[featureID.length];
		for (int i = 0; i < featureID.length; i++)
			fvector[i] = getFeatureValue(featureID[i]);
		return fvector;
	}

	public float[] getFeatureVector() {
		return this.fVals;
	}

	public float[] getExternalFeatureVector() {
		float[] ufVals = new float[this.fVals.length];
		System.arraycopy(this.fVals, 0, ufVals, 0, this.fVals.length);
		for (int i = 0; i < ufVals.length; i++)
			if (ufVals[i] > INFINITY + 1.0F)
				ufVals[i] = 0.0F;
		return ufVals;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void normalize(int[] fids, float[] norm) {
		for (int i = 0; i < fids.length; i++)
			if (norm[i] > 0.0D)
				this.fVals[fids[i]] /= norm[i];
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.label).append(" ").append("id:").append(this.id).append(" ");
		for (int i = 1; i < this.fVals.length; i++)
			if (this.fVals[i] > INFINITY + 1.0F)
				sb.append(i).append(":").append(this.fVals[i]).append(i == this.fVals.length - 1 ? "" : " ");
		sb.append(" ").append(this.description);
		return sb.toString();
	}

	public String toString2() {
		String output = new StringBuilder().append(this.label).append(" id:").append(this.id).append(" ").toString();
		for (int i = 1; i < this.fVals.length; i++)
			if (this.fVals[i] > INFINITY + 1.0F)
				output = new StringBuilder().append(output).append(i).append(":").append(this.fVals[i])
						.append(i == this.fVals.length - 1 ? "" : " ").toString();
		output = new StringBuilder().append(output).append(" ").append(this.description).toString();
		return output;
	}

	public void addFeatures(float[] values) {
		float[] tmp = new float[featureCount + 1 + values.length];
		System.arraycopy(this.fVals, 0, tmp, 0, this.fVals.length);
		Arrays.fill(tmp, this.fVals.length, featureCount + 1, INFINITY);
		System.arraycopy(values, 0, tmp, featureCount + 1, values.length);
		this.fVals = tmp;
	}

	public void setMartScore(float martScore) {
		this.martScore = martScore;
	}

	public float getMartScore() {
		return this.martScore;
	}

	public void setCached(double c) {
		this.cached = c;
	}

	public double getCached() {
		return this.cached;
	}

	public void resetCached() {
		this.cached = -100000000.0D;
	}
}