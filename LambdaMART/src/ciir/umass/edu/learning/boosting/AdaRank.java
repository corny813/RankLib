package ciir.umass.edu.learning.boosting;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.KeyValuePair;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class AdaRank extends Ranker {
	public static int nIteration = 500;
	public static double tolerance = 0.002D;
	public static boolean trainWithEnqueue = true;
	public static int maxSelCount = 5;

	protected Hashtable<Integer, Integer> usedFeatures = new Hashtable();
	protected double[] sweight = null;
	protected List<WeakRanker> rankers = null;
	protected List<Double> rweight = null;

	protected List<WeakRanker> bestModelRankers = null;
	protected List<Double> bestModelWeights = null;

	int lastFeature = -1;
	int lastFeatureConsecutiveCount = 0;
	boolean performanceChanged = false;
	List<Integer> featureQueue = null;
	protected double[] backupSampleWeight = null;
	protected double backupTrainScore = 0.0D;
	protected double lastTrainedScore = -1.0D;

	public AdaRank() {
	}

	public AdaRank(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	private void updateBestModelOnValidation() {
		this.bestModelRankers.clear();
		this.bestModelRankers.addAll(this.rankers);
		this.bestModelWeights.clear();
		this.bestModelWeights.addAll(this.rweight);
	}

	private WeakRanker learnWeakRanker() {
		double bestScore = -1.0D;
		WeakRanker bestWR = null;
		for (int k = 0; k < this.features.length; k++) {
			int i = this.features[k];
			if (this.featureQueue.contains(Integer.valueOf(i))) {
				continue;
			}
			if (this.usedFeatures.get(Integer.valueOf(i)) != null) {
				continue;
			}
			WeakRanker wr = new WeakRanker(i);
			double s = 0.0D;
			for (int j = 0; j < this.samples.size(); j++) {
				double t = this.scorer.score(wr.rank((RankList) this.samples.get(j))) * this.sweight[j];
				s += t;
			}

			if (bestScore >= s)
				continue;
			bestScore = s;
			bestWR = wr;
		}

		return bestWR;
	}

	private int learn(int startIteration, boolean withEnqueue) {
		int t = startIteration;
		for (; t <= nIteration; t++) {
			PRINT(new int[] { 7 }, new String[] { new StringBuilder().append(t).append("").toString() });

			WeakRanker bestWR = learnWeakRanker();
			if (bestWR == null) {
				break;
			}
			if (withEnqueue) {
				if (bestWR.getFID() == this.lastFeature) {
					this.featureQueue.add(Integer.valueOf(this.lastFeature));

					this.rankers.remove(this.rankers.size() - 1);
					this.rweight.remove(this.rweight.size() - 1);
					copy(this.backupSampleWeight, this.sweight);
					this.bestScoreOnValidationData = 0.0D;
					this.lastTrainedScore = this.backupTrainScore;
					PRINTLN(new int[] { 8, 9, 9, 9 }, new String[] {
							new StringBuilder().append(bestWR.getFID()).append("").toString(), "", "", "ROLLBACK" });
				} else {
					this.lastFeature = bestWR.getFID();

					copy(this.sweight, this.backupSampleWeight);
					this.backupTrainScore = this.lastTrainedScore;
				}
			} else {
				double num = 0.0D;
				double denom = 0.0D;
				for (int i = 0; i < this.samples.size(); i++) {
					double tmp = this.scorer.score(bestWR.rank((RankList) this.samples.get(i)));
					num += this.sweight[i] * (1.0D + tmp);
					denom += this.sweight[i] * (1.0D - tmp);
				}

				this.rankers.add(bestWR);
				double alpha_t = 0.5D * SimpleMath.ln(num / denom);
				this.rweight.add(Double.valueOf(alpha_t));

				double trainedScore = 0.0D;

				double total = 0.0D;
				for (int i = 0; i < this.samples.size(); i++) {
					double tmp = this.scorer.score(rank((RankList) this.samples.get(i)));
					total += Math.exp(-alpha_t * tmp);
					trainedScore += tmp;
				}
				trainedScore /= this.samples.size();
				double delta = trainedScore + tolerance - this.lastTrainedScore;
				String status = delta > 0.0D ? "OK" : "DAMN";

				if (!withEnqueue) {
					if (trainedScore != this.lastTrainedScore) {
						this.performanceChanged = true;
						this.lastFeatureConsecutiveCount = 0;

						this.usedFeatures.clear();
					} else {
						this.performanceChanged = false;
						if (this.lastFeature == bestWR.getFID()) {
							this.lastFeatureConsecutiveCount += 1;
							if (this.lastFeatureConsecutiveCount == maxSelCount) {
								status = "F. REM.";
								this.lastFeatureConsecutiveCount = 0;
								this.usedFeatures.put(Integer.valueOf(this.lastFeature), Integer.valueOf(1));
							}
						} else {
							this.lastFeatureConsecutiveCount = 0;

							this.usedFeatures.clear();
						}
					}
					this.lastFeature = bestWR.getFID();
				}

				PRINT(new int[] { 8, 9 },
						new String[] { new StringBuilder().append(bestWR.getFID()).append("").toString(),
								new StringBuilder().append(SimpleMath.round(trainedScore, 4)).append("").toString() });
				if ((t % 1 == 0) && (this.validationSamples != null)) {
					double scoreOnValidation = this.scorer.score(rank(this.validationSamples));
					if (scoreOnValidation > this.bestScoreOnValidationData) {
						this.bestScoreOnValidationData = scoreOnValidation;
						updateBestModelOnValidation();
					}
					PRINT(new int[] { 9, 9 }, new String[] {
							new StringBuilder().append(SimpleMath.round(scoreOnValidation, 4)).append("").toString(),
							status });
				} else {
					PRINT(new int[] { 9, 9 }, new String[] { "", status });
				}
				PRINTLN("");

				if (delta <= 0.0D) {
					this.rankers.remove(this.rankers.size() - 1);
					this.rweight.remove(this.rweight.size() - 1);
					break;
				}

				this.lastTrainedScore = trainedScore;
				for (int i = 0; i < this.sweight.length; i++)
					this.sweight[i] *= Math.exp(-alpha_t * this.scorer.score(rank((RankList) this.samples.get(i))))
							/ total;
			}
		}
		return t;
	}

	public void init() {
		PRINT("Initializing... ");

		this.usedFeatures.clear();

		this.sweight = new double[this.samples.size()];
		for (int i = 0; i < this.sweight.length; i++)
			this.sweight[i] = (1.0F / this.samples.size());
		this.backupSampleWeight = new double[this.sweight.length];
		copy(this.sweight, this.backupSampleWeight);
		this.lastTrainedScore = -1.0D;

		this.rankers = new ArrayList();
		this.rweight = new ArrayList();

		this.featureQueue = new ArrayList();

		this.bestScoreOnValidationData = 0.0D;
		this.bestModelRankers = new ArrayList();
		this.bestModelWeights = new ArrayList();

		PRINTLN("[Done]");
	}

	public void learn() {
		PRINTLN("---------------------------");
		PRINTLN("Training starts...");
		PRINTLN("--------------------------------------------------------");
		PRINTLN(new int[] { 7, 8, 9, 9, 9 },
				new String[] { "#iter", "Sel. F.",
						new StringBuilder().append(this.scorer.name()).append("-T").toString(),
						new StringBuilder().append(this.scorer.name()).append("-V").toString(), "Status" });
		PRINTLN("--------------------------------------------------------");

		if (trainWithEnqueue) {
			int t = learn(1, true);

			for (int i = this.featureQueue.size() - 1; i >= 0; i--) {
				this.featureQueue.remove(i);
				t = learn(t, false);
			}
		} else {
			learn(1, false);
		}

		if ((this.validationSamples != null) && (this.bestModelRankers.size() > 0)) {
			this.rankers.clear();
			this.rweight.clear();
			this.rankers.addAll(this.bestModelRankers);
			this.rweight.addAll(this.bestModelWeights);
		}

		this.scoreOnTrainingData = SimpleMath.round(this.scorer.score(rank(this.samples)), 4);
		PRINTLN("--------------------------------------------------------");
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
		for (int j = 0; j < this.rankers.size(); j++)
			score += ((Double) this.rweight.get(j)).doubleValue()
					* p.getFeatureValue(((WeakRanker) this.rankers.get(j)).getFID());
		return score;
	}

	public Ranker clone() {
		return new AdaRank();
	}

	public String toString() {
		String output = "";
		for (int i = 0; i < this.rankers.size(); i++)
			output = new StringBuilder().append(output).append(((WeakRanker) this.rankers.get(i)).getFID()).append(":")
					.append(this.rweight.get(i)).append(i == this.rankers.size() - 1 ? "" : " ").toString();
		return output;
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();
		output = new StringBuilder().append(output).append("## Iteration = ").append(nIteration).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## Train with enqueue: ")
				.append(trainWithEnqueue ? "Yes" : "No").append("\n").toString();
		output = new StringBuilder().append(output).append("## Tolerance = ").append(tolerance).append("\n").toString();
		output = new StringBuilder().append(output).append("## Max consecutive selection count = ").append(maxSelCount)
				.append("\n").toString();
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
			this.rweight = new ArrayList();
			this.rankers = new ArrayList();
			this.features = new int[keys.size()];
			for (int i = 0; i < keys.size(); i++) {
				this.features[i] = Integer.parseInt((String) keys.get(i));
				this.rankers.add(new WeakRanker(this.features[i]));
				this.rweight.add(Double.valueOf(Double.parseDouble((String) values.get(i))));
			}
		} catch (Exception ex) {
			System.out
					.println(new StringBuilder().append("Error in AdaRank::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of rounds: ").append(nIteration).toString());
		PRINTLN(new StringBuilder().append("Train with 'enequeue': ").append(trainWithEnqueue ? "Yes" : "No")
				.toString());
		PRINTLN(new StringBuilder().append("Tolerance: ").append(tolerance).toString());
		PRINTLN(new StringBuilder().append("Max Sel. Count: ").append(maxSelCount).toString());
	}

	public String name() {
		return "AdaRank";
	}
}