package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Evaluator;
import ciir.umass.edu.learning.Node;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.METRIC;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.ClusterAlgorithm;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

public class LambdaMART extends Ranker {
	public static int nTrees = 1000;
	public static float learningRate = 0.1F;

	public static int nThreshold = 1024;
	public static int nRoundToStopEarly = 1000;
	public static int nTreeLeaves = 10;
	public static int loopMax = 5;

	public static int minLeafSupport = 10;

	public static float clickDecreseWeight = 1.0F;
	public static String featureMinAndMaxFile = "";

	protected float[][] thresholds = (float[][]) null;
	protected Ensemble ensemble = null;
	protected float[] modelScores = null;
	protected float[][] modelScoresOnValidation = (float[][]) null;
	protected int bestModelOnValidation = Integer.MAX_VALUE-2;

	protected DataPoint[] martSamples = null;
	protected int[][] sortedIdx = (int[][]) null;
	protected FeatureHistogram hist = null;
	protected float[] pseudoResponses = null;
	public static TreeMap<Integer, Float> maxFeatureValue = new TreeMap();
	public static TreeMap<Integer, Float> minFeatureValue = new TreeMap();
	public static float samplingFeature = 1.0F;
	public int[] featureUseRate = null;
	public int[] noisePoint = null;

	protected FeatureHistogram histQuery = null;
	protected DataPoint[] martSamplesQuery = null;
	protected int[][] sortedIdxQuery = (int[][]) null;
	protected float[] pseudoResponsesQuery = null;
	protected int[] featureQuery = null;
	protected float[][] thresholdsQuery = (float[][]) null;

	protected int[] docScoreClass = null;
	protected ExcludeSortedDoc excludeSortedDoc = null;

	public double maxScore = 0.0D;
	public double currentScore = 0.0D;
	public float shrinkageLearnRate = 0.1F;
	public int currentTree = 0;
	public int initFeature = -1;
	public static String modelDir = "mbs.new/";
	protected int countQuery = 0;
	protected int countClick = 0;
	protected int countClickAndTop = 0;

	BufferedWriter out = null;
	DecimalFormat df = new DecimalFormat("0.0000");

	public static void main(String[] args) {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());
		ciir.umass.edu.learning.CoorAscent.nRestart = 1;

		Evaluator ev = new Evaluator(RANKER_TYPE.RANDOM_FOREST, METRIC.MAP, 10, METRIC.NDCG, 1);
		ciir.umass.edu.learning.boosting.AdaRank.trainWithEnqueue = false;
		ciir.umass.edu.learning.neuralnet.RankNet.nHiddenLayer = 1;
		ciir.umass.edu.learning.neuralnet.RankNet.nIteration = 300;
		ciir.umass.edu.learning.neuralnet.RankNet.nHiddenNodePerLayer = 5;

		ciir.umass.edu.learning.neuralnet.RankNet.learningRate = 0.0001D;

		ciir.umass.edu.learning.boosting.RankBoost.nIteration = 100;
		ciir.umass.edu.learning.boosting.RankBoost.nThreshold = 200;

		nThreshold = 2000;
		RFRanker.nBag = 100;

		String data_path = "C:\\Users\\wujunjie\\Downloads\\MQ2008\\MQ2008\\Fold1\\";
		ev.evaluate(data_path + "train.txt", data_path + "vali.txt", data_path + "test.txt", "");

		MyThreadPool.getInstance().shutdown();
	}

	public LambdaMART() {
	}

	public LambdaMART(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	public int loadMinAndMaxFeature() {
		minFeatureValue.clear();
		maxFeatureValue.clear();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(featureMinAndMaxFile)));
			String featureLine = null;
			while ((featureLine = in.readLine()) != null) {
				String[] featureSplit = featureLine.split("\t");
				int feature = Integer.parseInt(featureSplit[0]);

				float minFeature = Float.parseFloat(featureSplit[1]);
				float maxFeature = Float.parseFloat(featureSplit[2]);
				int featureid = ((Integer) Node.featureidMap.get(Integer.valueOf(feature))).intValue();
				minFeatureValue.put(Integer.valueOf(featureid), Float.valueOf(minFeature));
				maxFeatureValue.put(Integer.valueOf(featureid), Float.valueOf(maxFeature));
			}
			in.close();
			return 1;
		} catch (Exception e) {
			System.out.println("featureMinAndMaxFile not exist, calculate by trian data");
			calculateMinAndMaxFeature();
		}
		return 0;
	}

	public void calculateMinAndMaxFeature() {
		minFeatureValue.clear();
		maxFeatureValue.clear();
	}

	public void init() {
		PRINT("Initializing... \n");
		PRINT(new StringBuilder().append("click query weight adjust ").append(clickDecreseWeight).append("\n")
				.toString());

		int dpCount = 0;
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			dpCount += rl.size();
		}

		this.maxScore = this.scorer.maxScore(this.samples);
		this.noisePoint = new int[dpCount];
		Arrays.fill(this.noisePoint, 0);

		this.featureUseRate = new int[this.features.length + 1];
		for (int i = 0; i < this.features.length + 1; i++) {
			this.featureUseRate[i] = 1;
		}

		int clickIndex = -1;
		for (int i = 0; i < this.features.length; i++) {
			if (((Integer) Node.idfeatureMap.get(Integer.valueOf(this.features[i]))).intValue() == 1230) {
				clickIndex = i;
				System.out.println(
						new StringBuilder().append("click feature is ").append(this.features[clickIndex]).toString());
			}
		}

		for (int i = 0; i < this.features.length; i++) {
			if (((Integer) Node.idfeatureMap.get(Integer.valueOf(this.features[i]))).intValue() == 7005) {
				FeatureHistogram.notuseFeature = this.features[i];
				this.initFeature = this.features[i];
				System.out.println(new StringBuilder().append("click feature is ")
						.append(FeatureHistogram.notuseFeature).toString());
			}
		}

		int current = 0;
		this.martSamples = new DataPoint[dpCount];
		this.modelScores = new float[dpCount];
		this.pseudoResponses = new float[dpCount];
		this.docScoreClass = new int[dpCount];
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			for (int j = 0; j < rl.size(); j++) {
				this.martSamples[(current + j)] = rl.get(j);
				this.modelScores[(current + j)] = 0.0F;

				this.pseudoResponses[(current + j)] = 0.0F;
			}

			current += rl.size();
		}
		computePseudoResponses();
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			for (int j = 0; j < rl.size(); j++) {
				rl.get(j).isClick = rl.isClick();
			}

		}

		this.sortedIdx = new int[this.features.length][];
		MyThreadPool p = MyThreadPool.getInstance();
		if (p.size() == 1) {
			sortSamplesByFeature(0, this.features.length - 1);
		} else {
			int[] partition = p.partition(this.features.length);
			for (int i = 0; i < partition.length - 1; i++) {
				p.execute(new SortWorker(this, partition[i], partition[(i + 1)] - 1));
			}
			p.await();
		}

		System.out.println("load feature min and max Value");
		loadMinAndMaxFeature();

		this.thresholds = new float[this.features.length][];
		System.out.println(new StringBuilder().append("nThresholds is ").append(nThreshold).toString());

		for (int f = 0; f < this.features.length; f++) {
			List<Float> values = new ArrayList<>();
			float fmax = (1.0F / -1.0F);
			float fmin = 3.4028235E+38F;
			for (int i = 0; i < this.martSamples.length; i++) {
				int k = this.sortedIdx[f][i];
				float fv = this.martSamples[k].getFeatureValue(this.features[f]);
				values.add(Float.valueOf(fv));
				if (fmax < fv) {
					fmax = fv;
				}
				if (fmin > fv) {
					fmin = fv;
				}

				int j = i + 1;
				while ((j < this.martSamples.length)
						&& (this.martSamples[this.sortedIdx[f][j]].getFeatureValue(this.features[f]) <= fv)) {
					j++;
				}
				i = j - 1;
			}

			if ((values.size() <= nThreshold) || (nThreshold == -1)) {
				this.thresholds[f] = new float[values.size() + 1];
				for (int i = 0; i < values.size(); i++) {
					this.thresholds[f][i] = ((Float) values.get(i)).floatValue();
				}
				this.thresholds[f][values.size()] = 3.4028235E+38F;
			} else {
				float step = Math.abs(fmax - fmin) / nThreshold;
				float fminOnline;
				float fmaxOnline;
				if ((maxFeatureValue.get(Integer.valueOf(this.features[f])) == null)
						|| (minFeatureValue.get(Integer.valueOf(this.features[f])) == null)) {
					fmaxOnline = 0.0F;
					fminOnline = 0.0F;
				} else {
					fmaxOnline = ((Float) maxFeatureValue.get(Integer.valueOf(this.features[f]))).floatValue();
					fminOnline = ((Float) minFeatureValue.get(Integer.valueOf(this.features[f]))).floatValue();
				}

				if (Math.abs(fminOnline - fmaxOnline) < 0.001F) {
					fmaxOnline = fmax;
					fminOnline = fmin;
				}

				if ((fmin < fminOnline) || (fmax > fmaxOnline)) {
					System.out.println(new StringBuilder().append("feature : ")
							.append(Node.idfeatureMap.get(Integer.valueOf(this.features[f]))).append("\t").append(fmin)
							.append("\t").append(fmax).append("\t").append(fminOnline).append("\t").append(fmaxOnline)
							.toString());
				}

				if (fmin < fminOnline) {
					fmin = fminOnline;
				}

				if (fmax > fmaxOnline) {
					fmax = fmaxOnline;
				}

				this.thresholds[f] = new float[nThreshold + 1];
				this.thresholds[f][0] = fmin;
				for (int j = 1; j < nThreshold; j++) {
					this.thresholds[f][j] = (this.thresholds[f][(j - 1)] + step);
				}
				this.thresholds[f][nThreshold] = 3.4028235E+38F;
			}
		}

		if (this.validationSamples != null) {
			this.modelScoresOnValidation = new float[this.validationSamples.size()][];
			for (int i = 0; i < this.validationSamples.size(); i++) {
				this.modelScoresOnValidation[i] = new float[((RankList) this.validationSamples.get(i)).size()];
				Arrays.fill(this.modelScoresOnValidation[i], 0.0F);
			}

		}

		FeatureHistogram.samplingRate = samplingFeature;
		this.hist = new FeatureHistogram();

		this.hist.construct(this.martSamples, this.pseudoResponses, this.sortedIdx, this.features, this.thresholds);

		this.sortedIdx = ((int[][]) null);

		this.sortedIdxQuery = ((int[][]) null);

		PRINTLN("[Done]");
	}

	public void learn() {
		this.ensemble = new Ensemble();

		Split newRoot = new Split();
		int newThreshold = 4;

		int nodesDecrease = 0;
		double BaseScore = 0.0D;

		for (int m = 0; m < nTrees; m++) {
			System.out.println(new StringBuilder().append("@@@@@@ ").append(m + 1).append(" @@@@@@").toString());

			this.currentTree = m;

			computePseudoResponses();

			this.hist.update(this.pseudoResponses);

			RegressionTree rt = new RegressionTree(nTreeLeaves, this.martSamples, this.pseudoResponses, this.hist,
					minLeafSupport);

			rt.fit_splitselect(1.0D);

			this.ensemble.add(rt, this.shrinkageLearnRate);

			updateTreeOutput(rt);

			rt.clearSamples();

			for (int i = 0; i < this.modelScores.length; i++) {
				this.modelScores[i] += this.shrinkageLearnRate * rt.eval(this.martSamples[i]);
				this.martSamples[i].setMartScore(this.modelScores[i]);
			}

			this.scoreOnTrainingData = computeModelScoreOnTraining();
			if (m == 0) {
				BaseScore = this.scoreOnTrainingData;
			}
			this.currentScore = this.scoreOnTrainingData;
			System.out.println(new StringBuilder().append(" training_score:"+df.format(this.scoreOnTrainingData)).append(" ")
					.append(" learn_rate:"+this.shrinkageLearnRate).toString());

			if (this.validationSamples != null) {
				for (int i = 0; i < this.modelScoresOnValidation.length; i++) {
					for (int j = 0; j < this.modelScoresOnValidation[i].length; j++) {
						this.modelScoresOnValidation[i][j] += this.shrinkageLearnRate
								* rt.eval(((RankList) this.validationSamples.get(i)).get(j));
					}

				}

				double score = computeModelScoreOnValidation();
				PRINT(new int[] { 20 },
						new String[] {new StringBuilder().append(SimpleMath.round(score, 4)).append("").toString() });

				if (score > this.bestScoreOnValidationData) {
					this.bestScoreOnValidationData = score;
					this.bestModelOnValidation = (this.ensemble.treeCount() - 1);
				}

			}

			if (((m + 1) % 10 == 0) && (m - this.bestModelOnValidation > nRoundToStopEarly)) {
				break;
			}

			if (nodesDecrease < 0) {
				nodesDecrease = 0;
			}
			System.out.println(new StringBuilder()
					.append(" train_score:"+df.format(BaseScore)).append(" ")
					.append(" current_score:"+df.format(this.currentScore)).append(" ")
					.append(" max_score:"+df.format(this.maxScore)).toString());

			System.gc();
		}

		System.err.println(new StringBuilder().append("treeCount: ").append(this.ensemble.treeCount()).toString());

		this.scoreOnTrainingData = this.scorer.score(rank(this.samples));

		PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on training data: ")
				.append(SimpleMath.round(this.scoreOnTrainingData, 4)).toString());
		if (this.validationSamples != null) {
			this.bestScoreOnValidationData = this.scorer.score(rank(this.validationSamples));
			PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on validation data: ")
					.append(SimpleMath.round(this.bestScoreOnValidationData, 4)).toString());
		}
	}

	public void learn_loop() {
		this.ensemble = new Ensemble();

		int loopNum = 0;
		while (loopNum < loopMax) {
			for (int m = 0; m < nTrees; m++) {
				System.out.println(
						new StringBuilder().append("@@@@@@@@@@").append(m + 1).append("@@@@@@@@@@").toString());
				this.currentTree = m;
				computePseudoResponses();
				this.hist.update(this.pseudoResponses);
				RegressionTree rt;
				if (loopNum == 0) {
					rt = new RegressionTree(nTreeLeaves, this.martSamples, this.pseudoResponses, this.hist,
							minLeafSupport);
					rt.fit((loopNum + 1) / loopMax);
					this.ensemble.add(rt, this.shrinkageLearnRate);
				} else {
					if (m >= this.ensemble.trees.size()) {
						break;
					}

					rt = (RegressionTree) this.ensemble.trees.get(m);
					for (int i = 0; i < this.modelScores.length; i++) {
						this.modelScores[i] -= this.shrinkageLearnRate * rt.eval(this.martSamples[i]);
					}
					if (this.validationSamples != null) {
						for (int i = 0; i < this.modelScoresOnValidation.length; i++) {
							for (int j = 0; j < this.modelScoresOnValidation[i].length; j++) {
								this.modelScoresOnValidation[i][j] -= this.shrinkageLearnRate
										* rt.eval(((RankList) this.validationSamples.get(i)).get(j));
							}
						}
					}

					rt.set(nTreeLeaves, this.martSamples, this.pseudoResponses, this.hist, minLeafSupport);
					rt.fit_with_existed_tree((loopNum + 1) / loopMax);
				}

				updateTreeOutput(rt);

				rt.clearSamples();

				for (int i = 0; i < this.modelScores.length; i++) {
					this.modelScores[i] += this.shrinkageLearnRate * rt.eval(this.martSamples[i]);
					this.martSamples[i].setMartScore(this.modelScores[i]);
				}

				this.scoreOnTrainingData = computeModelScoreOnTraining();
				this.currentScore = this.scoreOnTrainingData;

				if (this.validationSamples != null) {
					for (int i = 0; i < this.modelScoresOnValidation.length; i++) {
						for (int j = 0; j < this.modelScoresOnValidation[i].length; j++) {
							this.modelScoresOnValidation[i][j] += this.shrinkageLearnRate
									* rt.eval(((RankList) this.validationSamples.get(i)).get(j));
						}

					}

					double score = computeModelScoreOnValidation();
					PRINT(new int[] { 9 }, new String[] {
							new StringBuilder().append(SimpleMath.round(score, 4)).append("").toString() });

					if (score > this.bestScoreOnValidationData) {
						this.bestScoreOnValidationData = score;
						this.bestModelOnValidation = (this.ensemble.treeCount() - 1);
					}
				}
				if (m - this.bestModelOnValidation > nRoundToStopEarly) {
					break;
				}
				PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on training data: ")
						.append(SimpleMath.round(this.scoreOnTrainingData, 4)).toString());
				if (this.validationSamples != null) {
					this.bestScoreOnValidationData = this.scorer.score(rank(this.validationSamples));
					PRINTLN(new StringBuilder().append(this.scorer.name()).append(" on validation data: ")
							.append(SimpleMath.round(this.bestScoreOnValidationData, 4)).toString());
				}

				System.out.println(new StringBuilder().append(this.scoreOnTrainingData).append(" ")
						.append(this.shrinkageLearnRate).toString());
				System.out.println(new StringBuilder().append("score on train data: ").append(this.currentScore)
						.append(" ").append(this.maxScore).toString());

				System.gc();
			}

			if (loopNum == 0) {
				while (this.ensemble.treeCount() > this.bestModelOnValidation + 1) {
					this.ensemble.remove(this.ensemble.treeCount() - 1);
				}
			}

			loopNum++;
		}
	}

	public int findThresholdId(int featureId, float threshold) {
		int id = 0;
		for (int i = 0; i < this.features.length; i++) {
			if (this.features[i] == featureId) {
				id = i;
			}
		}
		for (int i = 0; i < this.thresholds[id].length; i++) {
			if (threshold <= this.thresholds[id][i]) {
				return i;
			}
		}
		return 0;
	}

	public double eval(DataPoint dp) {
		return this.ensemble.eval(dp);
	}

	public Ranker clone() {
		return new LambdaMART();
	}

	public String toString() {
		return this.ensemble.toString();
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();

		output = new StringBuilder().append(output).append("## No. of trees = ").append(this.ensemble.treeCount())
				.append("\n").toString();
		output = new StringBuilder().append(output).append("## No. of leaves = ").append(nTreeLeaves).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## No. of threshold candidates = ").append(nThreshold)
				.append("\n").toString();
		output = new StringBuilder().append(output).append("## Learning rate = ").append(learningRate).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("## Stop early = ").append(nRoundToStopEarly).append("\n")
				.toString();
		output = new StringBuilder().append(output).append("\n").toString();
		output = new StringBuilder().append(output).append(toString()).toString();
		return output;
	}

	public void load(String fn) {
		try {
			System.out.println("MART IN");
			String content = "";
			String model = "";
			StringBuilder sb = new StringBuilder();

			Scanner scanner = new Scanner(new FileInputStream(fn));
			while (scanner.hasNext()) {
				content = scanner.nextLine().trim();
				if ((content.length() == 0) || (content.indexOf("##") == 0)) {
					continue;
				}

				sb.append(content);
			}
			scanner.close();
			model = sb.toString();

			this.ensemble = new Ensemble(model);
			this.ensemble.chectFeatureValue(minFeatureValue, maxFeatureValue);
		} catch (Exception ex) {
			System.out.println(
					new StringBuilder().append("Error in LambdaMART::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of trees: ").append(nTrees).toString());
		PRINTLN(new StringBuilder().append("No. of leaves: ").append(nTreeLeaves).toString());
		PRINTLN(new StringBuilder().append("No. of threshold candidates: ").append(nThreshold).toString());
		PRINTLN(new StringBuilder().append("Learning rate: ").append(learningRate).toString());
		PRINTLN(new StringBuilder().append("Stop early: ").append(nRoundToStopEarly)
				.append(" rounds without performance gain on validation data").toString());
	}

	public String name() {
		return "LambdaMART";
	}

	public Ensemble getEnsemble() {
		return this.ensemble;
	}

	protected void computePseudoResponses() {
		MyThreadPool p = MyThreadPool.getInstance();
		this.countQuery = 0;
		this.countClick = 0;
		this.countClickAndTop = 0;
		if (p.size() == 1) {
			computePseudoResponses(0, this.samples.size() - 1, 0);
			return;
		}

		List workers = new ArrayList();

		int chunk = (this.samples.size() - 1) / p.size() + 1;
		int current = 0;
		for (int i = 0; i < p.size(); i++) {
			int start = i * chunk;
			int end = start + chunk - 1;
			if (end >= this.samples.size()) {
				end = this.samples.size() - 1;
			}

			LambdaComputationWorker wk = new LambdaComputationWorker(this, start, end, current);
			workers.add(wk);
			p.execute(wk);

			if (i < chunk - 1) {
				for (int j = start; j <= end; j++) {
					current += ((RankList) this.samples.get(j)).size();
				}
			}

		}

		p.await();
	}

	protected void computePseudoResponses(int start, int end, int current) {
		double maxScoreSub = 0.0D;

		for (int i = start; i <= end; i++) {
			RankList r = (RankList) this.samples.get(i);
			float[][] changes = computeMetricChange(i, current);
			double[] lambdas = new double[r.size()];
			double[] weights = new double[r.size()];
			double[] lambdasCandidate = new double[r.size()];
			Arrays.fill(lambdas, 0.0D);
			Arrays.fill(weights, 0.0D);
			Arrays.fill(lambdasCandidate, 0.0D);

			for (int j = 0; j < r.size(); j++) {
				DataPoint p1 = r.get(j);

				for (int k = 0; k < r.size(); k++) {
					if (j == k) {
						continue;
					}
					DataPoint p2 = r.get(k);

					double deltaNDCG = Math.abs(changes[j][k]);

					double exponent = Math.signum(p1.getLabel() - p2.getLabel())
							* (this.modelScores[(current + j)] - this.modelScores[(current + k)]);
					double rho = 1.0D / (1.0D + Math.exp(exponent));

					double lambda = rho * deltaNDCG * Math.signum(p1.getLabel() - p2.getLabel());
					double delta = rho * (1.0D - rho) * deltaNDCG;
					lambdas[j] += lambda;
					weights[j] += delta;
				}

			}

			this.countQuery += 1;
			if (r.get(0).isClick) {
				r.calculateSortedState(1);
				this.countClick += 1;
				if (r.isClickIsMaxLabelAndSortedTop()) {
					this.countClickAndTop += 1;
				}
			}

			for (int j = 0; j < r.size(); j++) {
				this.pseudoResponses[(current + j)] = (float) lambdas[j];
				r.get(j).setCached(weights[j]);
				if (Double.isNaN(this.pseudoResponses[(current + j)])) {
					System.out.println(new StringBuilder().append("pseudoResponse is nan ").append(current + j)
							.append(" ").append(lambdas[j]).toString());
				}

			}

			current += r.size();
		}
	}

	protected void computePseudoResponses_replace(int start, int end, int current) {
		for (int i = start; i <= end; i++) {
			RankList r = (RankList) this.samples.get(i);

			double[] lambdas = new double[r.size()];
			double[] weights = new double[r.size()];
			Arrays.fill(lambdas, 0.0D);
			Arrays.fill(weights, 0.0D);

			for (int j = 0; j < r.size(); j++) {
				DataPoint p1 = r.get(j);
				RankList tmpr = new RankList(r);
				tmpr.remove(j);

				for (int k = 0; k < tmpr.size(); k++) {
					if (k <= 5) {
						DataPoint p2 = tmpr.get(k);
						if (p1.getLabel() == p2.getLabel()) {
							continue;
						}
						int kk = k < j ? k : k + 1;

						double exponent = Math.signum(p1.getLabel() - p2.getLabel())
								* (this.modelScores[(current + j)] - this.modelScores[(current + kk)]);
						double rho = 1.0D / (1.0D + Math.exp(exponent)) * Math.signum(p1.getLabel() - p2.getLabel());
						double deltaNDCG = Math.abs(Math.pow(2.0D, p1.getLabel()) - Math.pow(2.0D, p2.getLabel()))
								/ SimpleMath.logBase2(k + 2);
						double lambda = rho * deltaNDCG;
						double delta = rho * (1.0D - rho) * deltaNDCG;
						lambdas[j] += lambda;
						weights[j] += delta;
					}
				}
			}
			for (int j = 0; j < r.size(); j++) {
				this.pseudoResponses[(current + j)] = (float) lambdas[j];
				r.get(j).setCached(weights[j]);
				if (i == 10)
					System.out.println(new StringBuilder().append("new lambda ").append(j).append(" ")
							.append(r.get(j).getLabel()).append(" ").append(lambdas[j]).append(" ")
							.append(this.modelScores[(current + j)]).toString());
			}
			current += r.size();
		}
	}

	protected void updateTreeOutput_file(RegressionTree rt, int m) {
		List leaves = rt.leaves();
		ClusterAlgorithm ca = new ClusterAlgorithm();
		try {
			this.out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new StringBuilder().append("output_").append(m).toString())));

			for (int i = 0; i < leaves.size(); i++) {
				float s1 = 0.0F;
				float s2 = 0.0F;
				Split s = (Split) leaves.get(i);
				int[] idx = s.getSamples();

				float[] leafPseudo = new float[idx.length];
				for (int j = 0; j < idx.length; j++) {
					int k = idx[j];
					s1 += this.pseudoResponses[k];
					s2 = (float) (s2 + this.martSamples[k].getCached());
					leafPseudo[j] = this.pseudoResponses[k];
				}

				float[] gaussianParm = ca.getLeafGaussianParm(leafPseudo, 10, 10, 0.9F);
				if ((s2 <= 1.0E-006D) && (s2 >= -1.0E-007D)) {
					s2 = 1.0E-006F;
				}

				System.out.println(new StringBuilder().append(s1 / s2).append(" ").append(gaussianParm[0]).append(" ")
						.append(gaussianParm[1]).toString());
				s1 = 0.0F;
				s2 = 0.0F;
				for (int j = 0; j < idx.length; j++) {
					int k = idx[j];
					if (Math.abs(this.pseudoResponses[k] - gaussianParm[0]) < gaussianParm[1] * 2.0F) {
						s1 += this.pseudoResponses[k];
						s2 = (float) (s2 + this.martSamples[k].getCached());
					}
				}
				if ((s2 <= 1.0E-006D) && (s2 >= -1.0E-007D)) {
					s2 = 1.0E-006F;
				}
				System.out.println(new StringBuilder().append(s1 / s2).append(" ").append(gaussianParm[0]).append(" ")
						.append(gaussianParm[1]).toString());
				s.setOutput(s1 / s2);
				this.out.write(new StringBuilder().append("leaf ").append(((Split) leaves.get(i)).getOutput())
						.append(" ").append(s1 / s2).append("\n").toString());
				for (int j = 0; j < idx.length; j++) {
					int k = idx[j];
					this.out.write(new StringBuilder().append(this.martSamples[k].getID()).append(" ").append(k)
							.append(" ").append(this.pseudoResponses[k]).append(" ")
							.append(this.martSamples[k].getCached()).append("\n").toString());
				}

				float offset = 0.0F;
				float varWeight = (s.getSqSumOriginLabel()
						- s.getSumOriginLabel() * s.getSumOriginLabel() / s.samples.length) / s.samples.length + offset;
				if (varWeight < 0.0D)
					varWeight = 0.0F;
				varWeight = (float) Math.sqrt(varWeight);
			}

			this.out.close();
		} catch (Exception e) {
			System.out.println("asdfasdfasdfasdf");
		}
	}

	protected void updateTreeOutput(RegressionTree rt) {
		List<Split> leaves = rt.leaves();
		ClusterAlgorithm ca = new ClusterAlgorithm();
		for (int i = 0; i < leaves.size(); i++) {
			float s1 = 0.0F;
			float s2 = 0.0F;
			Split s = (Split) leaves.get(i);
			int[] idx = s.getSamples();
			float[] leafPseudo = new float[idx.length];
			for (int j = 0; j < idx.length; j++) {
				int k = idx[j];
				s1 += this.pseudoResponses[k];
				s2 = (float) (s2 + this.martSamples[k].getCached());
				leafPseudo[j] = this.pseudoResponses[k];
			}

			if ((s2 <= 1.0E-006D) && (s2 >= -1.0E-007D)) {
				s2 = 1.0E-006F;
			}

			if (s.getIsSplit() == 0)
				;
			s.setOutput(s1 / s2);

			float offset = 0.0F;
			float varWeight = (s.getSqSumOriginLabel()
					- s.getSumOriginLabel() * s.getSumOriginLabel() / s.samples.length) / s.samples.length + offset;
			if (varWeight < 0.0D)
				varWeight = 0.0F;
			varWeight = (float) Math.sqrt(varWeight);
		}
	}

	public void getRelevanceResult() {
		int[] sortFeature = { 1028, 1029, 1153, 1154, 1155, 1156, 1157, 1158, 1159, 1160, 1177, 1178 };
		for (int i = 0; i < sortFeature.length; i++)
			;
		int featureCount = 0;
		for (int i = 0; i < sortFeature.length; i++) {
			if (Node.featureidMap.get(Integer.valueOf(sortFeature[i])) != null) {
				featureCount++;
			}
		}

		this.martSamplesQuery = new DataPoint[this.samples.size()];
		this.pseudoResponsesQuery = new float[this.samples.size()];
		this.featureQuery = new int[featureCount];
		this.thresholdsQuery = new float[featureCount][];
		for (int i = 0; i < sortFeature.length; i++) {
			if (Node.featureidMap.get(Integer.valueOf(sortFeature[i])) != null) {
				this.featureQuery[i] = ((Integer) Node.featureidMap.get(Integer.valueOf(sortFeature[i]))).intValue();
			}
		}
		for (int i = 0; i < this.featureQuery.length; i++) {
			for (int j = 0; j < this.features.length; j++) {
				if (this.featureQuery[i] == this.features[j]) {
					this.thresholdsQuery[i] = this.thresholds[j];
				}
			}
		}
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			this.martSamplesQuery[i] = rl.get(0);
			this.pseudoResponsesQuery[i] = rl.getSplitDifficulty();
		}
		this.sortedIdxQuery = new int[this.featureQuery.length][];
		for (int i = 0; i < this.featureQuery.length; i++)
			this.sortedIdxQuery[i] = sortSamplesByFeature(this.martSamplesQuery, this.featureQuery[i]);
	}

	public void updateRelevanceResult() {
		float maxQueryPseudo = 0.0F;
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			this.pseudoResponsesQuery[i] = rl.getSortQueryScore();
			if (maxQueryPseudo < this.pseudoResponsesQuery[i]) {
				maxQueryPseudo = this.pseudoResponsesQuery[i];
			}
			rl.calculateScoreClass();
		}
	}

	protected int[] sortSamplesByFeature(DataPoint[] samples, int fid) {
		double[] score = new double[samples.length];
		for (int i = 0; i < samples.length; i++) {
			score[i] = samples[i].getFeatureValue(fid);
		}
		int[] idx = MergeSorter.sort(score, true);
		return idx;
	}

	protected RankList rank(int rankListIndex, int current) {
		RankList orig = (RankList) this.samples.get(rankListIndex);
		float[] scores = new float[orig.size()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = this.modelScores[(current + i)];
		}
		int[] idx = MergeSorter.sort(scores, false);
		return new RankList(orig, idx);
	}

	protected float computeModelScoreOnTraining() {
		float s = 0.0F;
		int current = 0;
		for (int i = 0; i < this.samples.size(); i++) {
			s = (float) (s + this.scorer.score(rank(i, current)));
			current += ((RankList) this.samples.get(i)).size();
		}
		s /= this.samples.size();
		return s;
	}

	protected float computeModelScoreOnValidation() {
		float score = 0.0F;
		for (int i = 0; i < this.validationSamples.size(); i++) {
			int[] idx = MergeSorter.sort(this.modelScoresOnValidation[i], false);
			score = (float) (score + this.scorer.score(new RankList((RankList) this.validationSamples.get(i), idx)));
		}
		return score / this.validationSamples.size();
	}

	protected float[][] computeMetricChange(int rankListIndex, int current) {
		RankList orig = (RankList) this.samples.get(rankListIndex);
		float[] scores = new float[orig.size()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = this.modelScores[(current + i)];
		}

		int[] idx = MergeSorter.sort(scores, false);
		RankList rl = new RankList(orig, idx);
		float[][] changes = new float[orig.size()][];
		for (int i = 0; i < changes.length; i++) {
			changes[i] = new float[orig.size()];
		}

		if (this.currentTree < 100)
			;
		// double[][] c = this.scorer.swapChange2(rl);
		double[][] c = this.scorer.swapChange(rl);
		for (int i = 0; i < changes.length; i++) {
			for (int j = 0; j < changes.length; j++) {
				changes[idx[i]][idx[j]] = (float) c[i][j];
			}
		}
		return changes;
	}

	protected void sortSamplesByFeature(int fStart, int fEnd) {
		for (int i = fStart; i <= fEnd; i++)
			this.sortedIdx[i] = sortSamplesByFeature(this.martSamples, this.features[i]);
	}

	class LambdaComputationWorker implements Runnable {
		LambdaMART ranker = null;
		int rlStart = -1;
		int rlEnd = -1;
		int martStart = -1;

		LambdaComputationWorker(LambdaMART ranker, int rlStart, int rlEnd, int martStart) {
			this.ranker = ranker;
			this.rlStart = rlStart;
			this.rlEnd = rlEnd;
			this.martStart = martStart;
		}

		public void run() {
			this.ranker.computePseudoResponses(this.rlStart, this.rlEnd, this.martStart);
		}
	}

	class SortWorker implements Runnable {
		LambdaMART ranker = null;
		int start = -1;
		int end = -1;

		SortWorker(LambdaMART ranker, int start, int end) {
			this.ranker = ranker;
			this.start = start;
			this.end = end;
		}

		public void run() {
			this.ranker.sortSamplesByFeature(this.start, this.end);
		}
	}
}