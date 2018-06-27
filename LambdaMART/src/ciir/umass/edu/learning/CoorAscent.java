package ciir.umass.edu.learning;

import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.KeyValuePair;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoorAscent extends Ranker {
	public static int nRestart = 2;
	public static int nMaxIteration = 25;
	public static double stepBase = 0.05D;
	public static double stepScale = 2.0D;
	public static double tolerance = 0.001D;
	public static boolean regularized = false;
	public static double slack = 0.001D;

	protected double[] weight = null;

	protected int current_feature = -1;
	protected double weight_change = -1.0D;

	public CoorAscent() {
	}

	public CoorAscent(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	public void init() {
		PRINT("Initializing... ");
		this.weight = new double[this.features.length];
		for (int i = 0; i < this.weight.length; i++)
			this.weight[i] = (1.0F / this.features.length);
		PRINTLN("[Done]");
	}

	public void learn() {
		double[] regVector = new double[this.weight.length];
		copy(this.weight, regVector);

		double[] bestModel = null;
		double bestModelScore = 0.0D;

		int[] sign = { 1, -1 };

		PRINTLN("---------------------------");
		PRINTLN("Training starts...");
		PRINTLN("---------------------------");

		for (int r = 0; r < nRestart; r++) {
			PRINTLN(new StringBuilder().append("[+] Random restart #").append(r + 1).append("/").append(nRestart)
					.append("...").toString());
			int consecutive_fails = 0;

			for (int i = 0; i < this.weight.length; i++)
				this.weight[i] = (1.0F / this.features.length);
			this.current_feature = -1;
			double startScore = this.scorer.score(rank(this.samples));

			double bestScore = startScore;
			double[] bestWeight = new double[this.weight.length];
			copy(this.weight, bestWeight);

			while (((this.weight.length > 1) && (consecutive_fails < this.weight.length - 1))
					|| ((this.weight.length == 1) && (consecutive_fails == 0))) {
				PRINTLN("Shuffling features' order... [Done.]");
				PRINTLN("Optimizing weight vector... ");
				PRINTLN("------------------------------");
				PRINTLN(new int[] { 7, 8, 7 }, new String[] { "Feature", "weight", this.scorer.name() });
				PRINTLN("------------------------------");

				int[] fids = getShuffledFeatures();

				for (int i = 0; i < fids.length; i++) {
					this.current_feature = fids[i];

					double origWeight = this.weight[fids[i]];
					double bestWeightValue = origWeight;
					boolean succeeds = false;
					for (int s = 0; s < sign.length; s++) {
						double step = 0.001D;
						if ((origWeight != 0.0D) && (step > 0.5D * Math.abs(origWeight)))
							step = stepBase * Math.abs(origWeight);
						double totalStep = step;
						for (int j = 0; j < nMaxIteration; j++) {
							double w = origWeight + totalStep * sign[s];
							this.weight_change = (w - this.weight[fids[i]]);
							this.weight[fids[i]] = w;
							double score = this.scorer.score(rank(this.samples));
							if (regularized) {
								double penalty = slack * getDistance(this.weight, regVector);
								score -= penalty;
							}

							if (score > bestScore) {
								bestScore = score;
								bestWeightValue = this.weight[fids[i]];
								succeeds = true;

								String bw = new StringBuilder().append(bestWeightValue > 0.0D ? "+" : "")
										.append(SimpleMath.round(bestWeightValue, 4)).toString();
								PRINTLN(new int[] { 7, 8, 7 },
										new String[] {
												new StringBuilder().append(this.features[fids[i]]).append("")
														.toString(),
												new StringBuilder().append(bw).append("").toString(),
												new StringBuilder().append(SimpleMath.round(bestScore, 4)).append("")
														.toString() });
							}
							step *= stepScale;
							totalStep += step;
						}
						if (succeeds) {
							break;
						}
						this.weight_change = (origWeight - this.weight[fids[i]]);
						updateCached();

						this.weight[fids[i]] = origWeight;
					}

					if (succeeds) {
						this.weight_change = (bestWeightValue - this.weight[fids[i]]);
						updateCached();
						this.weight[fids[i]] = bestWeightValue;
						consecutive_fails = 0;

						double sum = normalize(this.weight);
						scaleCached(sum);
						copy(this.weight, bestWeight);
					} else {
						consecutive_fails++;
						this.weight_change = (origWeight - this.weight[fids[i]]);
						updateCached();

						this.weight[fids[i]] = origWeight;
					}
				}
				PRINTLN("------------------------------");

				if (bestScore - startScore < tolerance) {
					break;
				}
			}
			if ((bestModel != null) && (bestScore <= bestModelScore))
				continue;
			bestModelScore = bestScore;
			bestModel = bestWeight;
		}

		copy(bestModel, this.weight);
		this.current_feature = -1;
		this.scoreOnTrainingData = SimpleMath.round(this.scorer.score(rank(this.samples)), 4);
		PRINTLN("---------------------------------");
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

	public RankList rank(RankList rl) {
		double[] score = new double[rl.size()];
		if (this.current_feature == -1) {
			for (int i = 0; i < rl.size(); i++) {
				for (int j = 0; j < this.features.length; j++)
					score[i] += this.weight[j] * rl.get(i).getFeatureValue(this.features[j]);
				rl.get(i).setCached(score[i]);
			}
		} else {
			for (int i = 0; i < rl.size(); i++) {
				score[i] = (rl.get(i).getCached()
						+ this.weight_change * rl.get(i).getFeatureValue(this.features[this.current_feature]));
				rl.get(i).setCached(score[i]);
			}
		}
		int[] idx = MergeSorter.sort(score, false);
		return new RankList(rl, idx);
	}

	public double eval(DataPoint p) {
		double score = 0.0D;
		for (int i = 0; i < this.features.length; i++)
			score += this.weight[i] * p.getFeatureValue(this.features[i]);
		return score;
	}

	public Ranker clone() {
		return new CoorAscent();
	}

	public String toString() {
		String output = "";
		for (int i = 0; i < this.weight.length; i++)
			output = new StringBuilder().append(output).append(this.features[i]).append(":").append(this.weight[i])
					.append(i == this.weight.length - 1 ? "" : " ").toString();
		return output;
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();
		output = new StringBuilder().append(output).append("## Restart = ").append(nRestart).append("\n").toString();
		output = new StringBuilder().append(output).append("## MaxIteration = ").append(nMaxIteration).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## StepBase = ").append(stepBase).append("\n").toString();
		output = new StringBuilder().append(output).append("## StepScale = ").append(stepScale).append("\n").toString();
		output = new StringBuilder().append(output).append("## Tolerance = ").append(tolerance).append("\n").toString();
		output = new StringBuilder().append(output).append("## Regularized = ").append(regularized).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## Slack = ").append(slack).append("\n").toString();
		output = new StringBuilder().append(output).append(toString()).toString();
		return output;
	}

	public void load(String fn) {
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));

			KeyValuePair kvp = null;
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("##") == 0))
					continue;
				kvp = new KeyValuePair(content);
			}

			in.close();

			List keys = kvp.keys();
			List values = kvp.values();
			this.weight = new double[keys.size()];
			this.features = new int[keys.size()];
			for (int i = 0; i < keys.size(); i++) {
				this.features[i] = Integer.parseInt((String) keys.get(i));
				this.weight[i] = Double.parseDouble((String) values.get(i));
			}
		} catch (Exception ex) {
			System.out.println(
					new StringBuilder().append("Error in CoorAscent::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of random restarts: ").append(nRestart).toString());
		PRINTLN(new StringBuilder().append("No. of iterations to search in each direction: ").append(nMaxIteration)
				.toString());
		PRINTLN(new StringBuilder().append("Tolerance: ").append(tolerance).toString());
		if (regularized)
			PRINTLN(new StringBuilder().append("Reg. param: ").append(slack).toString());
		else
			PRINTLN("Regularization: No");
	}

	public String name() {
		return "Coordinate Ascent";
	}

	private void updateCached() {
		for (int j = 0; j < this.samples.size(); j++) {
			RankList rl = (RankList) this.samples.get(j);
			for (int i = 0; i < rl.size(); i++) {
				double score = rl.get(i).getCached()
						+ this.weight_change * rl.get(i).getFeatureValue(this.features[this.current_feature]);
				rl.get(i).setCached(score);
			}
		}
	}

	private void scaleCached(double sum) {
		for (int j = 0; j < this.samples.size(); j++) {
			RankList rl = (RankList) this.samples.get(j);
			for (int i = 0; i < rl.size(); i++)
				rl.get(i).setCached(rl.get(i).getCached() / sum);
		}
	}

	private int[] getShuffledFeatures() {
		int[] fids = new int[this.features.length];
		List l = new ArrayList();
		for (int i = 0; i < this.features.length; i++)
			l.add(Integer.valueOf(i));
		Collections.shuffle(l);
		for (int i = 0; i < l.size(); i++)
			fids[i] = ((Integer) l.get(i)).intValue();
		return fids;
	}

	private double getDistance(double[] w1, double[] w2) {
		double s1 = 0.0D;
		double s2 = 0.0D;
		for (int i = 0; i < w1.length; i++) {
			s1 += Math.abs(w1[i]);
			s2 += Math.abs(w2[i]);
		}
		double dist = 0.0D;
		for (int i = 0; i < w1.length; i++) {
			double t = w1[i] / s1 - w2[i] / s2;
			dist += t * t;
		}
		return Math.sqrt(dist);
	}

	private double normalize(double[] weights) {
		double sum = 0.0D;
		for (int j = 0; j < weights.length; j++)
			sum += Math.abs(weights[j]);
		for (int j = 0; j < weights.length; j++)
			weights[j] /= sum;
		return sum;
	}

	public void copyModel(CoorAscent ranker) {
		this.weight = new double[this.features.length];
		if (ranker.weight.length != this.weight.length) {
			System.out.println("These two models use different feature set!!");
			System.exit(1);
		}
		copy(ranker.weight, this.weight);
		PRINTLN("Model loaded.");
	}

	public double distance(CoorAscent ca) {
		return getDistance(this.weight, ca.weight);
	}
}