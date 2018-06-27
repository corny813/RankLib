package ciir.umass.edu.learning.boosting;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class RankBoost extends Ranker {
	public static int nIteration = 300;
	public static int nThreshold = 10;

	protected double[][][] sweight = (double[][][]) null;
	protected double[][] potential = (double[][]) null;
	protected List<List<int[]>> sortedSamples = new ArrayList();
	protected double[][] thresholds = (double[][]) null;
	protected int[][] tSortedIdx = (int[][]) null;
	protected Hashtable<Integer, Integer> usedFeatures = new Hashtable();

	protected List<RBWeakRanker> wRankers = null;
	protected List<Double> rWeight = null;

	protected List<RBWeakRanker> bestModelRankers = new ArrayList();
	protected List<Double> bestModelWeights = new ArrayList();

	private double R_t = 0.0D;
	private double Z_t = 1.0D;
	private int totalCorrectPairs = 0;

	public RankBoost() {
	}

	public RankBoost(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	private int[] reorder(RankList rl, int fid) {
		double[] score = new double[rl.size()];
		for (int i = 0; i < rl.size(); i++)
			score[i] = rl.get(i).getFeatureValue(fid);
		int[] idx = MergeSorter.sort(score, false);
		return idx;
	}

	private void updatePotential() {
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			for (int j = 0; j < rl.size(); j++) {
				double p = 0.0D;
				for (int k = j + 1; k < rl.size(); k++)
					p += this.sweight[i][j][k];
				for (int k = 0; k < j; k++)
					p -= this.sweight[i][k][j];
				this.potential[i][j] = p;
			}
		}
	}

	private RBWeakRanker learnWeakRanker() {
		int bestFid = -1;
		double maxR = -10.0D;
		double bestThreshold = -1.0D;
		for (int i = 0; i < this.features.length; i++) {
			List sSortedIndex = (List) this.sortedSamples.get(i);
			int[] idx = this.tSortedIdx[i];
			int[] last = new int[this.samples.size()];
			for (int j = 0; j < this.samples.size(); j++) {
				last[j] = -1;
			}
			double r = 0.0D;
			for (int j = 0; j < idx.length; j++) {
				double t = this.thresholds[i][idx[j]];

				for (int k = 0; k < this.samples.size(); k++) {
					RankList rl = (RankList) this.samples.get(k);
					int[] sk = (int[]) sSortedIndex.get(k);
					for (int l = last[k] + 1; l < rl.size(); l++) {
						DataPoint p = rl.get(sk[l]);
						if (p.getFeatureValue(this.features[i]) <= t)
							break;
						r += this.potential[k][sk[l]];
						last[k] = l;
					}

				}

				if (r <= maxR)
					continue;
				maxR = r;
				bestThreshold = t;
				bestFid = this.features[i];
			}
		}

		if (bestFid == -1) {
			return null;
		}
		this.R_t = (this.Z_t * maxR);

		return new RBWeakRanker(bestFid, bestThreshold);
	}

	public void init() {
		PRINT("Initializing... ");

		this.wRankers = new ArrayList();
		this.rWeight = new ArrayList();

		this.totalCorrectPairs = 0;
		for (int i = 0; i < this.samples.size(); i++) {
			this.samples.set(i, ((RankList) this.samples.get(i)).getCorrectRanking());
			RankList rl = (RankList) this.samples.get(i);
			for (int j = 0; j < rl.size() - 1; j++) {
				for (int k = rl.size() - 1; (k >= j + 1) && (rl.get(j).getLabel() > rl.get(k).getLabel()); k--) {
					this.totalCorrectPairs += 1;
				}
			}
		}
		this.sweight = new double[this.samples.size()][][];
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			this.sweight[i] = new double[rl.size()][];
			for (int j = 0; j < rl.size() - 1; j++) {
				this.sweight[i][j] = new double[rl.size()];
				for (int k = j + 1; k < rl.size(); k++) {
					if (rl.get(j).getLabel() > rl.get(k).getLabel())
						this.sweight[i][j][k] = (1.0D / this.totalCorrectPairs);
					else {
						this.sweight[i][j][k] = 0.0D;
					}
				}
			}
		}
		this.potential = new double[this.samples.size()][];
		for (int i = 0; i < this.samples.size(); i++) {
			this.potential[i] = new double[((RankList) this.samples.get(i)).size()];
		}
		if (nThreshold <= 0) {
			int count = 0;
			for (int i = 0; i < this.samples.size(); i++) {
				count += ((RankList) this.samples.get(i)).size();
			}
			this.thresholds = new double[this.features.length][];
			for (int i = 0; i < this.features.length; i++) {
				this.thresholds[i] = new double[count];
			}
			int c = 0;
			for (int i = 0; i < this.samples.size(); i++) {
				RankList rl = (RankList) this.samples.get(i);
				for (int j = 0; j < rl.size(); j++) {
					for (int k = 0; k < this.features.length; k++)
						this.thresholds[k][c] = rl.get(j).getFeatureValue(this.features[k]);
					c++;
				}
			}
		} else {
			double[] fmax = new double[this.features.length];
			double[] fmin = new double[this.features.length];
			for (int i = 0; i < this.features.length; i++) {
				fmax[i] = -1000000.0D;
				fmin[i] = 1000000.0D;
			}

			for (int i = 0; i < this.samples.size(); i++) {
				RankList rl = (RankList) this.samples.get(i);
				for (int j = 0; j < rl.size(); j++) {
					for (int k = 0; k < this.features.length; k++) {
						double f = rl.get(j).getFeatureValue(this.features[k]);
						if (f > fmax[k])
							fmax[k] = f;
						if (f < fmin[k]) {
							fmin[k] = f;
						}
					}
				}
			}
			this.thresholds = new double[this.features.length][];
			for (int i = 0; i < this.features.length; i++) {
				double step = Math.abs(fmax[i] - fmin[i]) / nThreshold;
				this.thresholds[i] = new double[nThreshold + 1];
				this.thresholds[i][0] = fmax[i];
				for (int j = 1; j < nThreshold; j++)
					this.thresholds[i][j] = (this.thresholds[i][(j - 1)] - step);
				this.thresholds[i][nThreshold] = (fmin[i] - 100000000.0D);
			}

		}

		this.tSortedIdx = new int[this.features.length][];
		for (int i = 0; i < this.features.length; i++) {
			this.tSortedIdx[i] = MergeSorter.sort(this.thresholds[i], false);
		}

		for (int i = 0; i < this.features.length; i++) {
			List idx = new ArrayList();
			for (int j = 0; j < this.samples.size(); j++)
				idx.add(reorder((RankList) this.samples.get(j), this.features[i]));
			this.sortedSamples.add(idx);
		}
		PRINTLN("[Done]");
	}

	public void learn() {
		PRINTLN("------------------------------------------");
		PRINTLN("Training starts...");
		PRINTLN("--------------------------------------------------------------------");
		PRINTLN(new int[] { 7, 8, 9, 9, 9, 9 },
				new String[] { "#iter", "Sel. F.", "Threshold", "Error",
						new StringBuilder().append(this.scorer.name()).append("-T").toString(),
						new StringBuilder().append(this.scorer.name()).append("-V").toString() });
		PRINTLN("--------------------------------------------------------------------");

		for (int t = 1; t <= nIteration; t++) {
			updatePotential();

			RBWeakRanker wr = learnWeakRanker();
			if (wr == null) {
				break;
			}
			double alpha_t = 0.5D * SimpleMath.ln((this.Z_t + this.R_t) / (this.Z_t - this.R_t));

			this.wRankers.add(wr);
			this.rWeight.add(Double.valueOf(alpha_t));

			this.Z_t = 0.0D;
			for (int i = 0; i < this.samples.size(); i++) {
				RankList rl = (RankList) this.samples.get(i);
				double[][] D_t = new double[rl.size()][];
				for (int j = 0; j < rl.size() - 1; j++) {
					D_t[j] = new double[rl.size()];
					for (int k = j + 1; k < rl.size(); k++) {
						D_t[j][k] = (this.sweight[i][j][k]
								* Math.exp(alpha_t * (wr.score(rl.get(k)) - wr.score(rl.get(j)))));
						this.Z_t += D_t[j][k];
					}
				}
				this.sweight[i] = D_t;
			}

			PRINT(new int[] { 7, 8, 9, 9 },
					new String[] { new StringBuilder().append(t).append("").toString(),
							new StringBuilder().append(wr.getFid()).append("").toString(),
							new StringBuilder().append(SimpleMath.round(wr.getThreshold(), 4)).append("").toString(),
							new StringBuilder().append(SimpleMath.round(this.R_t, 4)).append("").toString() });
			if (t % 1 == 0) {
				PRINT(new int[] { 9 }, new String[] { new StringBuilder()
						.append(SimpleMath.round(this.scorer.score(rank(this.samples)), 4)).append("").toString() });
				if (this.validationSamples != null) {
					double score = this.scorer.score(rank(this.validationSamples));
					if (score > this.bestScoreOnValidationData) {
						this.bestScoreOnValidationData = score;
						this.bestModelRankers.clear();
						this.bestModelRankers.addAll(this.wRankers);
						this.bestModelWeights.clear();
						this.bestModelWeights.addAll(this.rWeight);
					}
					PRINT(new int[] { 9 }, new String[] {
							new StringBuilder().append(SimpleMath.round(score, 4)).append("").toString() });
				}
			}
			PRINTLN("");

			for (int i = 0; i < this.samples.size(); i++) {
				RankList rl = (RankList) this.samples.get(i);
				for (int j = 0; j < rl.size() - 1; j++) {
					for (int k = j + 1; k < rl.size(); k++)
						this.sweight[i][j][k] /= this.Z_t;
				}
			}
			System.gc();
		}

		if ((this.validationSamples != null) && (this.bestModelRankers.size() > 0)) {
			this.wRankers.clear();
			this.rWeight.clear();
			this.wRankers.addAll(this.bestModelRankers);
			this.rWeight.addAll(this.bestModelWeights);
		}

		this.scoreOnTrainingData = SimpleMath.round(this.scorer.score(rank(this.samples)), 4);
		PRINTLN("--------------------------------------------------------------------");
		PRINTLN("Finished sucessfully.");
		PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on training data: ")
				.append(this.scoreOnTrainingData).toString());
		if (this.validationSamples != null) {
			this.bestScoreOnValidationData = this.scorer.score(rank(this.validationSamples));
			PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on validation data: ")
					.append(SimpleMath.round(this.bestScoreOnValidationData, 4)).toString());
		}
		PRINTLN("---------------------------------");
	}

	public double eval(DataPoint p) {
		double score = 0.0D;
		for (int j = 0; j < this.wRankers.size(); j++)
			score += ((Double) this.rWeight.get(j)).doubleValue() * ((RBWeakRanker) this.wRankers.get(j)).score(p);
		return score;
	}

	public Ranker clone() {
		return new RankBoost();
	}

	public String toString() {
		String output = "";
		for (int i = 0; i < this.wRankers.size(); i++)
			output = new StringBuilder().append(output).append(((RBWeakRanker) this.wRankers.get(i)).toString())
					.append(":").append(this.rWeight.get(i)).append(i == this.rWeight.size() - 1 ? "" : " ").toString();
		return output;
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();
		output = new StringBuilder().append(output).append("## Iteration = ").append(nIteration).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## No. of threshold candidates = ").append(nThreshold)
				.append("\n").toString();
		output = new StringBuilder().append(output).append(toString()).toString();
		return output;
	}

	public void load(String fn) {
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));
			do {
				if ((content = in.readLine()) == null)
					break;
				content = content.trim();
			} while ((content.length() == 0) || (content.indexOf("##") == 0));

			in.close();

			this.rWeight = new ArrayList();
			this.wRankers = new ArrayList();

			int idx = content.lastIndexOf("#");
			if (idx != -1) {
				content = content.substring(0, idx).trim();
			}
			String[] fs = content.split(" ");
			for (int i = 0; i < fs.length; i++) {
				fs[i] = fs[i].trim();
				if (fs[i].compareTo("") == 0)
					continue;
				String[] strs = fs[i].split(":");
				int fid = Integer.parseInt(strs[0]);
				double threshold = Double.parseDouble(strs[1]);
				double weight = Double.parseDouble(strs[2]);
				this.rWeight.add(Double.valueOf(weight));
				this.wRankers.add(new RBWeakRanker(fid, threshold));
			}

			this.features = new int[this.rWeight.size()];
			for (int i = 0; i < this.rWeight.size(); i++)
				this.features[i] = ((RBWeakRanker) this.wRankers.get(i)).getFid();
		} catch (Exception ex) {
			System.out.println(
					new StringBuilder().append("Error in RankBoost::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of rounds: ").append(nIteration).toString());
		PRINTLN(new StringBuilder().append("No. of threshold candidates: ").append(nThreshold).toString());
	}

	public String name() {
		return "RankBoost";
	}
}