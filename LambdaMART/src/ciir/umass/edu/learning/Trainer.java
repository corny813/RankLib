package ciir.umass.edu.learning;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.MyThreadPool;

public class Trainer {
	private Ranker ranker = null;
	private Ranker rankerOnline = null;
	protected RankerFactory rFact = new RankerFactory();
	protected RANKER_TYPE type = RANKER_TYPE.LAMBDAMART;
	protected RANKER_TYPE typeRanknet = RANKER_TYPE.RANKNET;
	protected MetricScorerFactory mFact = new MetricScorerFactory();
	protected MetricScorer trainScorer = null;
	protected MetricScorer testScorer = null;
	public static boolean letor = false;
	public static boolean mustHaveRelDoc = false;
	List<RankList> train = null;
	List<RankList> validation = null;
	List<RankList> test = null;

	private String trainMetric = "ERR@5";
	private String testMetric = "ERR@3";

	private int nTrees = 500;

	private int nTreeLeaves = 20;

	private int nRoundToStopEarly = 50;
	private String trainFile;
	private String validationFile;
	private String testFile;
	private String modelFile = "k.model";
	private String modelOnline = "k2.model";
	public double trainScore = 0.0D;
	DecimalFormat df = new DecimalFormat("0.0000");

	public Trainer(String trainFile) {
		this.trainFile = trainFile;

		ciir.umass.edu.learning.tree.LambdaMART.nTrees = this.nTrees;
		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = this.nTreeLeaves;
		ciir.umass.edu.learning.tree.LambdaMART.nRoundToStopEarly = this.nRoundToStopEarly;

		System.out.println("readFeatures");

		this.train = readInput(trainFile);

		this.trainScorer = this.mFact.createScorer(this.trainMetric);
		this.testScorer = this.mFact.createScorer(this.testMetric);
	}

	public Trainer(String modelFile, String testFile) {
		this.modelFile = modelFile;
		this.testFile = testFile;

		this.test = readInput(testFile);
		this.testScorer = this.mFact.createScorer(this.testMetric);
		this.ranker = this.rFact.loadRanker(modelFile);
		System.out.println("load model done");
	}

	public Trainer(String modelFile, String testFile, String metric2T) {
		this.modelFile = modelFile;
		this.testFile = testFile;

		this.test = readInput(testFile);
		if (metric2T.equals(""))
			this.testScorer = this.mFact.createScorer(this.testMetric);
		else
			this.testScorer = this.mFact.createScorer(metric2T);
		this.ranker = this.rFact.loadRanker(modelFile);
		System.out.println("load model done");
	}

	public Trainer(String modelFile, List<RankList> test2) {
		this.modelFile = modelFile;
		this.test = test2;
		this.testScorer = this.mFact.createScorer(this.testMetric);
		this.ranker = this.rFact.loadRanker(modelFile);
	}

	public Trainer(String modelFile, String modelOnline, List<RankList> test2) {
		this.modelFile = modelFile;
		this.modelOnline = modelOnline;
		this.ranker = this.rFact.loadRanker(modelFile);
		this.rankerOnline = this.rFact.loadRanker(modelOnline);
		this.test = test2;
		this.testScorer = this.mFact.createScorer(this.testMetric);
	}

	public Trainer(String trainFile, String validationFile, String testFile, String modelFile) {
		this.trainFile = trainFile;
		this.validationFile = validationFile;
		this.testFile = testFile;
		this.modelFile = modelFile;

		readFeatures();

		this.trainScorer = this.mFact.createScorer(this.trainMetric);
		this.testScorer = this.mFact.createScorer(this.testMetric);
	}

	public Trainer(String trainFile, String validationFile, String testFile, String modelFile, String modelOnline) {
		this.trainFile = trainFile;
		this.validationFile = validationFile;
		this.testFile = testFile;
		this.modelFile = modelFile;
		this.rankerOnline = this.rFact.loadRanker(modelOnline);

		readFeatures();

		this.trainScorer = this.mFact.createScorer(this.trainMetric);
		this.testScorer = this.mFact.createScorer(this.testMetric);
	}

	public Trainer(String trainFile, String validationFile, String testFile, String modelFile, String modelOnline,
			String metric2t, String metric2T) {
		this.trainFile = trainFile;
		this.validationFile = validationFile;
		this.testFile = testFile;
		this.modelFile = modelFile;
		this.rankerOnline = this.rFact.loadRanker(modelOnline);

		readFeatures();

		if (metric2t.equals("")) {
			this.trainScorer = this.mFact.createScorer(this.trainMetric);
		} else {
			this.trainScorer = this.mFact.createScorer(metric2t);
		}
		if (metric2T.equals("")) {
			this.testScorer = this.mFact.createScorer(this.testMetric);
		} else {
			this.testScorer = this.mFact.createScorer(metric2T);
		}
	}

	public double calc(int[] features) {
		train(features);

		double rankScore = evaluate(this.ranker, this.test);
		MyThreadPool.getInstance().shutdown();
		return rankScore;
	}

	public double score() {
		double rankScore = evaluate(this.ranker, this.test);
		System.out.println("test score\t" + df.format(rankScore));
		return rankScore;
	}

	public double scoreToFile(String file, String type) {
		double rankScore = evaluateToFile(this.ranker, this.test, file, type);
		System.out.println("test score\t" + df.format(rankScore));
		return rankScore;
	}

	public double scoreWin(String useFunction) {
		int win = 0;
		int lose = 0;
		int draw = 0;
		double scoreWinThreshold = 0.1D;
		for (RankList rankList : this.test) {
			RankList l = null;
			RankList l2 = null;
			l = this.ranker.rank(rankList);
			l2 = this.rankerOnline.rank(rankList);
			double score = 0.0D;
			double score2 = 0.0D;
			if (useFunction.compareTo("ERR3") == 0) {
				score = this.testScorer.scoreERR3(l);
				score2 = this.testScorer.scoreERR3(l2);
			} else if (useFunction.compareTo("WithNotLabel") == 0) {
				score = this.testScorer.scoreWithNotLabel(l);
				score2 = this.testScorer.scoreWithNotLabel(l2);
			} else if (useFunction.compareTo("WithoutNotLabel") == 0) {
				score = this.testScorer.scoreWithoutNotLabel(l);
				score2 = this.testScorer.scoreWithoutNotLabel(l2);
			} else {
				score = this.testScorer.score(l);
				score2 = this.testScorer.score(l2);
			}
			if ((score < 0.0D) || (score2 < 0.0D)) {
				continue;
			}
			double diff = score - score2;
			if (diff >= scoreWinThreshold) {
				win++;
			} else if (diff <= -scoreWinThreshold) {
				lose++;
			} else {
				draw++;
			}
		}
		double winRatio = (win + 1.0D * draw / 2.0D) / (win + lose + draw);
		System.out.println("win ratio\t" + df.format(winRatio) + "\tcompare num:\t" + (win + lose + draw));
		return winRatio;
	}

	public double score2(double cof) {
		double rankScore = evaluate(cof, this.test);
		System.out.println("test score\t" + df.format(cof) + "\t" + df.format(rankScore));
		return rankScore;
	}

	public double score(String testFile) {
		List test2 = readInput(testFile);
		double rankScore = evaluate(this.ranker, test2);
		System.out.println("test score\t" + df.format(rankScore));
		return rankScore;
	}

	public void train(int[] features) {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());
		this.ranker = this.rFact.createRanker(this.type, this.train, features);
		this.ranker.set(this.trainScorer);
		this.ranker.setValidationSet(this.validation);
		this.ranker.init();
		this.ranker.learn();
		MyThreadPool.getInstance().shutdown();
	}

	public void train(Node node) {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());
		System.out.println("train");
		System.out.println(node.size());
		System.out.println(node);

		HashSet<Integer> set1 = node.getSet();
		int[] features = new int[set1.size()];
		int i = 0;
		for (Integer integer : set1) {
			features[i] = ((Integer) Node.featureidMap.get(integer)).intValue();
			i++;
		}

		long start = System.currentTimeMillis();

		this.ranker = this.rFact.createRanker(this.type, this.train, features);
		this.ranker.set(this.trainScorer);
		this.ranker.setValidationSet(this.validation);
		this.ranker.init();
		this.ranker.learn();
		this.trainScore = this.ranker.getScoreOnTrainingData();

		long end = System.currentTimeMillis();
		double time = (end - start) / 1000.0D;
		System.out.println("time cost\t" + time);

		MyThreadPool.getInstance().shutdown();
	}

	public void trainRanknet(Node node) {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());
		System.out.println("train");
		System.out.println(node.size());
		System.out.println(node);

		HashSet<Integer> set1 = node.getSet();
		int[] features = new int[set1.size()];
		int i = 0;
		for (Integer integer : set1) {
			features[i] = integer.intValue();
			i++;
		}

		long start = System.currentTimeMillis();

		this.ranker = this.rFact.createRanker(this.typeRanknet, this.train, features);
		this.ranker.set(this.trainScorer);
		this.ranker.setValidationSet(this.validation);
		this.ranker.init();
		this.ranker.learn();
		this.ranker.save("ranknet.model");

		long end = System.currentTimeMillis();
		double time = (end - start) / 1000.0D;
		System.out.println("time cost\t" + time);

		MyThreadPool.getInstance().shutdown();
	}

	public double calcWithValidation(int[] features) {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());

		this.ranker = this.rFact.createRanker(this.type, this.train, features);
		this.ranker.setValidationSet(this.validation);
		this.ranker.set(this.trainScorer);
		this.ranker.init();
		this.ranker.learn();

		double rankScore = evaluate(this.ranker, this.test);
		MyThreadPool.getInstance().shutdown();
		return rankScore;
	}

	public double evaluate(Ranker ranker, List<RankList> rl) {
		List l = rl;
		if (ranker != null) {
			l = ranker.rank(rl);
		}
		return this.testScorer.score(l);
	}

	public double evaluateToFile(Ranker ranker, List<RankList> rl, String file, String type) {
		List l = rl;
		if (ranker != null) {
			l = ranker.rank(rl);
		}

		if (type.compareTo("ERR3") == 0) {
			return this.testScorer.scoreERR3(l, file);
		}
		if (type.compareTo("WithNotLabel") == 0)
			return this.testScorer.scoreWithNotLabel(l, file);
		if (type.compareTo("WithoutNotLabel") == 0) {
			return this.testScorer.scoreWithoutNotLabel(l, file);
		}
		return this.testScorer.score(l);
	}

	public double evaluate(double cof, List<RankList> rl) {
		List l = rl;
		if (this.ranker != null) {
			l = rank(cof, rl);
		}
		return this.testScorer.score(l);
	}

	public RankList rank(double cof, RankList rl) {
		double[] scores = new double[rl.size()];
		for (int i = 0; i < rl.size(); i++)
			scores[i] = (cof * this.ranker.eval(rl.get(i)) + (1.0D - cof) * this.rankerOnline.eval(rl.get(i)));
		int[] idx = MergeSorter.sort(scores, false);
		return new RankList(rl, idx);
	}

	public List<RankList> rank(double cof, List<RankList> l) {
		List<RankList> ll = new ArrayList<>();
		for (int i = 0; i < l.size(); i++)
			ll.add(rank(cof, (RankList) l.get(i)));
		return ll;
	}

	private void readFeatures() {
		this.train = readInput(this.trainFile);
		this.validation = readInput(this.validationFile);
		this.test = readInput(this.testFile);
	}

	public List<RankList> readInput(String inputFile) {
		FeatureManager fm = new FeatureManager();
		List samples = fm.read(inputFile, letor, mustHaveRelDoc);
		return samples;
	}

	public void save(int round) {
		this.ranker.save(this.modelFile + "." + round);
	}

	public void save(String fix) {
		this.ranker.save(this.modelFile + "." + fix);
	}
}