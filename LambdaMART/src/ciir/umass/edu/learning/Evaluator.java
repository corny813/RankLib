package ciir.umass.edu.learning;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.features.Normalizer;
import ciir.umass.edu.features.SumNormalizor;
import ciir.umass.edu.features.ZScoreNormalizor;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.Neuron;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.ERRScorer;
import ciir.umass.edu.metric.METRIC;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.FileUtils;
import ciir.umass.edu.utilities.LinearComputer;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.SimpleMath;
import ciir.umass.edu.utilities.Sorter;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Evaluator {
	public static boolean letor = false;
	public static boolean mustHaveRelDoc = false;
	public static boolean normalize = false;
	public static Normalizer nml = new SumNormalizor();
	public static String modelFile = "";
	public static String modelToLoad = "";

	public static String qrelFile = "";

	public static String newFeatureFile = "";
	public static boolean keepOrigFeatures = false;
	public static int topNew = 2000;

	protected RankerFactory rFact = new RankerFactory();
	protected MetricScorerFactory mFact = new MetricScorerFactory();

	protected MetricScorer trainScorer = null;
	protected MetricScorer testScorer = null;
	protected RANKER_TYPE type = RANKER_TYPE.MART;

	protected List<LinearComputer> lcList = new ArrayList();

	public static void main(String[] args) {
		String[] rType = { "MART", "RankNet", "RankBoost", "AdaRank", "Coordinate Ascent", "LambdaRank", "LambdaMART",
				"ListNet", "Random Forests" };
		RANKER_TYPE[] rType2 = { RANKER_TYPE.MART, RANKER_TYPE.RANKNET, RANKER_TYPE.RANKBOOST, RANKER_TYPE.ADARANK,
				RANKER_TYPE.COOR_ASCENT, RANKER_TYPE.LAMBDARANK, RANKER_TYPE.LAMBDAMART, RANKER_TYPE.LISTNET,
				RANKER_TYPE.RANDOM_FOREST };

		String trainFile = "";
		String featureDescriptionFile = "";
		double ttSplit = 0.0D;
		double tvSplit = 0.0D;
		int foldCV = -1;
		String validationFile = "";
		String testFile = "";
		int rankerType = 4;
		String trainMetric = "ERR@10";
		String testMetric = "";
		normalize = false;
		String savedModelFile = "";
		String rankFile = "";
		boolean printIndividual = false;

		String indriRankingFile = "";
		String scoreFile = "";

		if (args.length < 2) {
			System.out.println("Usage: java -jar RankLib.jar <Params>");
			System.out.println("Params:");
			System.out.println("  [+] Training (+ tuning and evaluation)");
			System.out.println("\t-train <file>\t\tTraining data");
			System.out.println("\t-ranker <type>\t\tSpecify which ranking algorithm to use");
			System.out.println("\t\t\t\t0: MART (gradient boosted regression tree)");
			System.out.println("\t\t\t\t1: RankNet");
			System.out.println("\t\t\t\t2: RankBoost");
			System.out.println("\t\t\t\t3: AdaRank");
			System.out.println("\t\t\t\t4: Coordinate Ascent");
			System.out.println("\t\t\t\t6: LambdaMART");
			System.out.println("\t\t\t\t7: ListNet");
			System.out.println("\t\t\t\t8: Random Forests");
			System.out.println(
					"\t[ -feature <file> ]\tFeature description file: list features to be considered by the learner, each on a separate line");
			System.out.println("\t\t\t\tIf not specified, all features will be used.");

			System.out.println(new StringBuilder()
					.append("\t[ -metric2t <metric> ]\tMetric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k (default=")
					.append(trainMetric).append(")").toString());
			System.out.println(
					"\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default to the same as specified for -metric2t)");
			System.out.println(new StringBuilder()
					.append("\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=")
					.append((int) SimpleMath.logBase2(ERRScorer.MAX)).append(", i.e. 5-point scale {0,1,2,3,4})")
					.toString());

			System.out.println(
					"\t[ -test <file> ]\tSpecify if you want to evaluate the trained model on this data (default=unspecified)");
			System.out.println(
					"\t[ -validate <file> ]\tSpecify if you want to tune your system on the validation data (default=unspecified)");
			System.out.println(
					"\t\t\t\tIf specified, the final model will be the one that performs best on the validation data");
			System.out.println("\t[ -tvs <x \\in [0..1]> ]\tSet train-validation split to be (x)(1.0-x)");
			System.out.println(
					"\t[ -tts <x \\in [0..1]> ]\tSet train-test split to be (x)(1.0-x). -tts will override -tvs");
			System.out.println(
					"\t[ -kcv <k> ]\t\tSpecify if you want to perform k-fold cross validation using ONLY the specified training data (default=NoCV)");

			System.out.println(
					"\t[ -norm <method>]\tNormalize feature vectors (default=no-normalization). Method can be:");
			System.out.println("\t\t\t\tsum: normalize each feature by the sum of all its values");
			System.out.println("\t\t\t\tzscore: normalize each feature by its mean/standard deviation");

			System.out.println("\t[ -save <model> ]\tSave the learned model to the specified file (default=not-save)");

			System.out.println("\t[ -silent ]\t\tDo not print progress messages (which are printed by default)");

			System.out.println("");
			System.out.println("    [-] RankNet-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -epoch <T> ]\t\tThe number of epochs to train (default=")
					.append(RankNet.nIteration).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -layer <layer> ]\tThe number of hidden layers (default=")
					.append(RankNet.nHiddenLayer).append(")").toString());
			System.out.println(
					new StringBuilder().append("\t[ -node <node> ]\tThe number of hidden nodes per layer (default=")
							.append(RankNet.nHiddenNodePerLayer).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -lr <rate> ]\t\tLearning rate (default=")
					.append(new DecimalFormat("###.########").format(RankNet.learningRate)).append(")").toString());

			System.out.println("");
			System.out.println("    [-] RankBoost-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -round <T> ]\t\tThe number of rounds to train (default=")
					.append(RankBoost.nIteration).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -tc <k> ]\t\tNumber of threshold candidates to search. -1 to use all feature values (default=")
					.append(RankBoost.nThreshold).append(")").toString());

			System.out.println("");
			System.out.println("    [-] AdaRank-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -round <T> ]\t\tThe number of rounds to train (default=")
					.append(AdaRank.nIteration).append(")").toString());
			System.out.println("\t[ -noeq ]\t\tTrain without enqueuing too-strong features (default=unspecified)");
			System.out.println(new StringBuilder()
					.append("\t[ -tolerance <t> ]\tTolerance between two consecutive rounds of learning (default=")
					.append(AdaRank.tolerance).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -max <times> ]\tThe maximum number of times can a feature be consecutively selected without changing performance (default=")
					.append(AdaRank.maxSelCount).append(")").toString());

			System.out.println("");
			System.out.println("    [-] Coordinate Ascent-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -r <k> ]\t\tThe number of random restarts (default=")
					.append(CoorAscent.nRestart).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -i <iteration> ]\tThe number of iterations to search in each dimension (default=")
					.append(CoorAscent.nMaxIteration).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -tolerance <t> ]\tPerformance tolerance between two solutions (default=")
					.append(CoorAscent.tolerance).append(")").toString());
			System.out.println("\t[ -reg <slack> ]\tRegularization parameter (default=no-regularization)");

			System.out.println("");
			System.out.println("    [-] {MART, LambdaMART}-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -tree <t> ]\t\tNumber of trees (default=")
					.append(LambdaMART.nTrees).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=")
					.append(LambdaMART.nTreeLeaves).append(")").toString());
			System.out.println(
					new StringBuilder().append("\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=")
							.append(LambdaMART.learningRate).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=")
					.append(LambdaMART.nThreshold).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -mls <n> ]\t\tMin leaf support -- minimum #samples each leaf has to contain (default=")
					.append(LambdaMART.minLeafSupport).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -estop <e> ]\t\tStop early when no improvement is observed on validaton data in e consecutive rounds (default=")
					.append(LambdaMART.nRoundToStopEarly).append(")").toString());

			System.out.println("");
			System.out.println("    [-] ListNet-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -epoch <T> ]\t\tThe number of epochs to train (default=")
					.append(ListNet.nIteration).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -lr <rate> ]\t\tLearning rate (default=")
					.append(new DecimalFormat("###.########").format(ListNet.learningRate)).append(")").toString());

			System.out.println("");
			System.out.println("    [-] Random Forests-specific parameters");
			System.out.println(new StringBuilder().append("\t[ -bag <r> ]\t\tNumber of bags (default=")
					.append(RFRanker.nBag).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -srate <r> ]\t\tSub-sampling rate (default=")
					.append(RFRanker.subSamplingRate).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -frate <r> ]\t\tFeature sampling rate (default=")
					.append(RFRanker.featureSamplingRate).append(")").toString());
			int type = RFRanker.rType.ordinal() - RANKER_TYPE.MART.ordinal();
			System.out.println(new StringBuilder().append("\t[ -rtype <type> ]\tRanker to bag (default=").append(type)
					.append(", i.e. ").append(rType[type]).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -tree <t> ]\t\tNumber of trees in each bag (default=")
					.append(RFRanker.nTrees).append(")").toString());
			System.out.println(new StringBuilder().append("\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=")
					.append(RFRanker.nTreeLeaves).append(")").toString());
			System.out.println(
					new StringBuilder().append("\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=")
							.append(RFRanker.learningRate).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=")
					.append(RFRanker.nThreshold).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -mls <n> ]\t\tMin leaf support -- minimum #samples each leaf has to contain (default=")
					.append(RFRanker.minLeafSupport).append(")").toString());

			System.out.println("");
			System.out.println("  [+] Testing previously saved models");
			System.out.println("\t-load <model>\t\tThe model to load");
			System.out.println(
					"\t-test <file>\t\tTest data to evaluate the model (specify either this or -rank but not both)");
			System.out.println(
					"\t-rank <file>\t\tRank the samples in the specified file (specify either this or -test but not both)");
			System.out.println(new StringBuilder()
					.append("\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default=")
					.append(trainMetric).append(")").toString());
			System.out.println(new StringBuilder()
					.append("\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=")
					.append((int) SimpleMath.logBase2(ERRScorer.MAX)).append(", i.e. 5-point scale {0,1,2,3,4})")
					.toString());
			System.out.println(
					"\t[ -score <file>]\tStore ranker's score for each object being ranked (has to be used with -rank)");

			System.out.println(
					"\t[ -idv ]\t\tPrint model performance (in test metric) on individual ranked lists (has to be used with -test)");
			System.out.println("\t[ -norm ]\t\tNormalize feature vectors (similar to -norm for training/tuning)");

			System.out.println("");
			return;
		}

		MyThreadPool.init(Runtime.getRuntime().availableProcessors());

		for (int i = 0; i < args.length; i++) {
			if (args[i].compareTo("-train") == 0) {
				i++;
				trainFile = args[i];
			} else if (args[i].compareTo("-ranker") == 0) {
				i++;
				rankerType = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-feature") == 0) {
				i++;
				featureDescriptionFile = args[i];
			} else if (args[i].compareTo("-metric2t") == 0) {
				i++;
				trainMetric = args[i];
			} else if (args[i].compareTo("-metric2T") == 0) {
				i++;
				testMetric = args[i];
			} else if (args[i].compareTo("-gmax") == 0) {
				i++;
				ERRScorer.MAX = Math.pow(2.0D, Double.parseDouble(args[i]));
			} else if (args[i].compareTo("-qrel") == 0) {
				i++;
				qrelFile = args[i];
			} else if (args[i].compareTo("-tts") == 0) {
				i++;
				ttSplit = Double.parseDouble(args[i]);
			} else if (args[i].compareTo("-tvs") == 0) {
				i++;
				tvSplit = Double.parseDouble(args[i]);
			} else if (args[i].compareTo("-kcv") == 0) {
				i++;
				foldCV = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-validate") == 0) {
				i++;
				validationFile = args[i];
			} else if (args[i].compareTo("-test") == 0) {
				i++;
				testFile = args[i];
			} else if (args[i].compareTo("-norm") == 0) {
				normalize = true;
				i++;
				String n = args[i];
				if (n.compareTo("sum") == 0) {
					nml = new SumNormalizor();
				} else if (n.compareTo("zscore") == 0) {
					nml = new ZScoreNormalizor();
				} else {
					System.out.println(new StringBuilder().append("Unknown normalizor: ").append(n).toString());
					System.out.println("System will now exit.");
					System.exit(1);
				}
			} else if (args[i].compareTo("-save") == 0) {
				i++;
				modelFile = args[i];
			} else if (args[i].compareTo("-silent") == 0) {
				Ranker.verbose = false;
			} else if (args[i].compareTo("-load") == 0) {
				i++;
				savedModelFile = args[i];
				modelToLoad = args[i];
			} else if (args[i].compareTo("-idv") == 0) {
				printIndividual = true;
			} else if (args[i].compareTo("-rank") == 0) {
				i++;
				rankFile = args[i];
			} else if (args[i].compareTo("-score") == 0) {
				i++;
				scoreFile = args[i];
			} else if (args[i].compareTo("-epoch") == 0) {
				i++;
				RankNet.nIteration = Integer.parseInt(args[i]);
				ListNet.nIteration = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-layer") == 0) {
				i++;
				RankNet.nHiddenLayer = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-node") == 0) {
				i++;
				RankNet.nHiddenNodePerLayer = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-lr") == 0) {
				i++;
				RankNet.learningRate = Double.parseDouble(args[i]);
				ListNet.learningRate = Neuron.learningRate;
			} else if (args[i].compareTo("-tc") == 0) {
				i++;
				RankBoost.nThreshold = Integer.parseInt(args[i]);
				LambdaMART.nThreshold = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-noeq") == 0) {
				AdaRank.trainWithEnqueue = false;
			} else if (args[i].compareTo("-max") == 0) {
				i++;
				AdaRank.maxSelCount = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-r") == 0) {
				i++;
				CoorAscent.nRestart = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-i") == 0) {
				i++;
				CoorAscent.nMaxIteration = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-round") == 0) {
				i++;
				RankBoost.nIteration = Integer.parseInt(args[i]);
				AdaRank.nIteration = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-reg") == 0) {
				i++;
				CoorAscent.slack = Double.parseDouble(args[i]);
				CoorAscent.regularized = true;
			} else if (args[i].compareTo("-tolerance") == 0) {
				i++;
				AdaRank.tolerance = Double.parseDouble(args[i]);
				CoorAscent.tolerance = Double.parseDouble(args[i]);
			} else if (args[i].compareTo("-tree") == 0) {
				i++;
				LambdaMART.nTrees = Integer.parseInt(args[i]);
				RFRanker.nTrees = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-leaf") == 0) {
				i++;
				LambdaMART.nTreeLeaves = Integer.parseInt(args[i]);
				RFRanker.nTreeLeaves = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-shrinkage") == 0) {
				i++;
				LambdaMART.learningRate = Float.parseFloat(args[i]);
				RFRanker.learningRate = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-mls") == 0) {
				i++;
				LambdaMART.minLeafSupport = Integer.parseInt(args[i]);
				RFRanker.minLeafSupport = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-estop") == 0) {
				i++;
				LambdaMART.nRoundToStopEarly = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-bag") == 0) {
				i++;
				RFRanker.nBag = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-srate") == 0) {
				i++;
				RFRanker.subSamplingRate = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-frate") == 0) {
				i++;
				RFRanker.featureSamplingRate = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-letor") == 0) {
				letor = true;
			} else if (args[i].compareTo("-nf") == 0) {
				i++;
				newFeatureFile = args[i];
			} else if (args[i].compareTo("-keep") == 0) {
				keepOrigFeatures = true;
			} else if (args[i].compareTo("-t") == 0) {
				i++;
				topNew = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-indri") == 0) {
				i++;
				indriRankingFile = args[i];
			} else if (args[i].compareTo("-hr") == 0) {
				mustHaveRelDoc = true;
			} else {
				System.out.println(
						new StringBuilder().append("Unknown command-line parameter: ").append(args[i]).toString());
				System.out.println("System will now exit.");
				System.exit(1);
			}
		}

		if (testMetric.compareTo("") == 0) {
			testMetric = trainMetric;
		}
		System.out.println("");

		System.out.println("[+] General Parameters:");
		System.out.println(new StringBuilder().append("LETOR 4.0 dataset: ").append(letor ? "Yes" : "No").toString());
		Evaluator e = new Evaluator(rType2[rankerType], trainMetric, testMetric);
		if (trainFile.compareTo("") != 0) {
			System.out.println(new StringBuilder().append("Training data:\t").append(trainFile).toString());

			if (foldCV != -1) {
				System.out.println(
						new StringBuilder().append("Cross validation: ").append(foldCV).append(" folds.").toString());
			} else {
				if (testFile.compareTo("") != 0)
					System.out.println(new StringBuilder().append("Test data:\t").append(testFile).toString());
				else if (ttSplit > 0.0D) {
					System.out.println(new StringBuilder().append("Train-Test split: ").append(ttSplit).toString());
				}
				if (validationFile.compareTo("") != 0)
					System.out.println(
							new StringBuilder().append("Validation data:\t").append(validationFile).toString());
				else if ((ttSplit <= 0.0D) && (tvSplit > 0.0D))
					System.out
							.println(new StringBuilder().append("Train-Validation split: ").append(tvSplit).toString());
			}
			System.out.println(new StringBuilder().append("Ranking method:\t").append(rType[rankerType]).toString());
			if (featureDescriptionFile.compareTo("") != 0)
				System.out.println(new StringBuilder().append("Feature description file:\t")
						.append(featureDescriptionFile).toString());
			else
				System.out.println("Feature description file:\tUnspecified. All features will be used.");
			System.out.println(new StringBuilder().append("Train metric:\t").append(trainMetric).toString());
			System.out.println(new StringBuilder().append("Test metric:\t").append(testMetric).toString());
			if ((trainMetric.toUpperCase().startsWith("ERR")) || (testMetric.toUpperCase().startsWith("ERR")))
				System.out.println(new StringBuilder().append("Highest relevance label (to compute ERR): ")
						.append((int) SimpleMath.logBase2(ERRScorer.MAX)).toString());
			if (qrelFile.compareTo("") != 0)
				System.out.println(new StringBuilder()
						.append("TREC-format relevance judgment (only affects MAP and NDCG scores): ").append(qrelFile)
						.toString());
			System.out.println(new StringBuilder().append("Feature normalization: ")
					.append(normalize ? nml.name() : "No").toString());
			if (modelFile.compareTo("") != 0) {
				System.out.println(new StringBuilder().append("Model file: ").append(modelFile).toString());
			}
			System.out.println("");
			System.out.println(
					new StringBuilder().append("[+] ").append(rType[rankerType]).append("'s Parameters:").toString());
			RankerFactory rf = new RankerFactory();

			rf.createRanker(rType2[rankerType]).printParameters();
			System.out.println("");

			if (foldCV != -1) {
				e.evaluate(trainFile, featureDescriptionFile, foldCV);
			} else if (ttSplit > 0.0D)
				e.evaluate(trainFile, validationFile, featureDescriptionFile, ttSplit);
			else if (tvSplit > 0.0D)
				e.evaluate(trainFile, tvSplit, testFile, featureDescriptionFile);
			else {
				e.evaluate(trainFile, validationFile, testFile, featureDescriptionFile);
			}
		} else {
			System.out.println(new StringBuilder().append("Model file:\t").append(savedModelFile).toString());
			System.out.println(new StringBuilder().append("Feature normalization: ")
					.append(normalize ? nml.name() : "No").toString());
			if (rankFile.compareTo("") != 0) {
				if (scoreFile.compareTo("") != 0)
					e.score(savedModelFile, rankFile, scoreFile);
				else if (indriRankingFile.compareTo("") != 0)
					e.rank(savedModelFile, rankFile, indriRankingFile);
				else
					e.rank(savedModelFile, rankFile);
			} else {
				System.out.println(new StringBuilder().append("Test metric:\t").append(testMetric).toString());
				if (testMetric.startsWith("ERR"))
					System.out.println(new StringBuilder().append("Highest relevance label (to compute ERR): ")
							.append((int) SimpleMath.logBase2(ERRScorer.MAX)).toString());
				if (savedModelFile.compareTo("") != 0) {
					e.test(savedModelFile, testFile, printIndividual);
				} else {
					e.test(testFile);
				}
			}
		}
		MyThreadPool.getInstance().shutdown();
	}

	public Evaluator(RANKER_TYPE rType, METRIC trainMetric, METRIC testMetric) {
		this.type = rType;
		this.trainScorer = this.mFact.createScorer(trainMetric);
		this.testScorer = this.mFact.createScorer(testMetric);
		if (qrelFile.compareTo("") != 0) {
			this.trainScorer.loadExternalRelevanceJudgment(qrelFile);
			this.testScorer.loadExternalRelevanceJudgment(qrelFile);
		}
	}

	public Evaluator(RANKER_TYPE rType, METRIC trainMetric, int trainK, METRIC testMetric, int testK) {
		this.type = rType;
		this.trainScorer = this.mFact.createScorer(trainMetric, trainK);
		this.testScorer = this.mFact.createScorer(testMetric, testK);
		if (qrelFile.compareTo("") != 0) {
			this.trainScorer.loadExternalRelevanceJudgment(qrelFile);
			this.testScorer.loadExternalRelevanceJudgment(qrelFile);
		}
	}

	public Evaluator(RANKER_TYPE rType, METRIC trainMetric, METRIC testMetric, int k) {
		this.type = rType;
		this.trainScorer = this.mFact.createScorer(trainMetric, k);
		this.testScorer = this.mFact.createScorer(testMetric, k);
		if (qrelFile.compareTo("") != 0) {
			this.trainScorer.loadExternalRelevanceJudgment(qrelFile);
			this.testScorer.loadExternalRelevanceJudgment(qrelFile);
		}
	}

	public Evaluator(RANKER_TYPE rType, METRIC metric, int k) {
		this.type = rType;
		this.trainScorer = this.mFact.createScorer(metric, k);
		if (qrelFile.compareTo("") != 0)
			this.trainScorer.loadExternalRelevanceJudgment(qrelFile);
		this.testScorer = this.trainScorer;
	}

	public Evaluator(RANKER_TYPE rType, String trainMetric, String testMetric) {
		this.type = rType;
		this.trainScorer = this.mFact.createScorer(trainMetric);
		this.testScorer = this.mFact.createScorer(testMetric);
		if (qrelFile.compareTo("") != 0) {
			this.trainScorer.loadExternalRelevanceJudgment(qrelFile);
			this.testScorer.loadExternalRelevanceJudgment(qrelFile);
		}
	}

	public List<RankList> readInput(String inputFile) {
		FeatureManager fm = new FeatureManager();
		List samples = fm.read(inputFile, letor, mustHaveRelDoc);
		return samples;
	}

	public int[] readFeature(String featureDefFile) {
		FeatureManager fm = new FeatureManager();
		int[] features = fm.getFeatureIDFromFile(featureDefFile);
		return features;
	}

	public void normalize(List<RankList> samples, int[] fids) {
		for (int i = 0; i < samples.size(); i++)
			nml.normalize((RankList) samples.get(i), fids);
	}

	public double evaluate(Ranker ranker, List<RankList> rl) {
		List l = rl;
		if (ranker != null)
			l = ranker.rank(rl);
		return this.testScorer.score(l);
	}

	public void evaluate(String trainFile, String validationFile, String testFile, String featureDefFile) {
		List train = readInput(trainFile);
		List validation = null;
		if (validationFile.compareTo("") != 0)
			validation = readInput(validationFile);
		List test = null;
		if (testFile.compareTo("") != 0)
			test = readInput(testFile);
		int[] features = readFeature(featureDefFile);
		if (features == null) {
			features = getFeatureFromSampleVector(train);
		}
		if (normalize) {
			normalize(train, features);
			if (validation != null)
				normalize(validation, features);
			if (test != null) {
				normalize(test, features);
			}

		}

		Ranker ranker = this.rFact.createRanker(this.type, train, features);
		ranker.set(this.trainScorer);
		ranker.setValidationSet(validation);
		ranker.init();
		ranker.learn();

		if (test != null) {
			double rankScore = evaluate(ranker, test);
			System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
					.append(SimpleMath.round(rankScore, 4)).toString());
		}
		if (modelFile.compareTo("") != 0) {
			System.out.println("");
			ranker.save(modelFile);
			System.out.println(new StringBuilder().append("Model saved to: ").append(modelFile).toString());
		}
	}

	public void evaluate(String sampleFile, String validationFile, String featureDefFile, double percentTrain) {
		List trainingData = new ArrayList();
		List testData = new ArrayList();
		int[] features = prepareSplit(sampleFile, featureDefFile, percentTrain, normalize, trainingData, testData);
		List validation = null;
		if (validationFile.compareTo("") != 0) {
			validation = readInput(validationFile);
		}
		Ranker ranker = this.rFact.createRanker(this.type, trainingData, features);
		ranker.set(this.trainScorer);
		ranker.setValidationSet(validation);
		ranker.init();
		ranker.learn();

		double rankScore = evaluate(ranker, testData);

		System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
				.append(SimpleMath.round(rankScore, 4)).toString());
		if (modelFile.compareTo("") != 0) {
			System.out.println("");
			ranker.save(modelFile);
			System.out.println(new StringBuilder().append("Model saved to: ").append(modelFile).toString());
		}
	}

	public void evaluate(String trainFile, double percentTrain, String testFile, String featureDefFile) {
		List train = new ArrayList();
		List validation = new ArrayList();
		int[] features = prepareSplit(trainFile, featureDefFile, percentTrain, normalize, train, validation);
		List test = null;
		if (testFile.compareTo("") != 0) {
			test = readInput(testFile);
		}
		Ranker ranker = this.rFact.createRanker(this.type, train, features);
		ranker.set(this.trainScorer);
		ranker.setValidationSet(validation);
		ranker.init();
		ranker.learn();

		if (test != null) {
			double rankScore = evaluate(ranker, test);
			System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
					.append(SimpleMath.round(rankScore, 4)).toString());
		}
		if (modelFile.compareTo("") != 0) {
			System.out.println("");
			ranker.save(modelFile);
			System.out.println(new StringBuilder().append("Model saved to: ").append(modelFile).toString());
		}
	}

	public void evaluate(String sampleFile, String featureDefFile, int nFold) {
		List trainingData = new ArrayList();
		List testData = new ArrayList();
		int[] features = prepareCV(sampleFile, featureDefFile, nFold, normalize, trainingData, testData);

		Ranker ranker = null;
		double origScore = 0.0D;
		double rankScore = 0.0D;
		double oracleScore = 0.0D;

		for (int i = 0; i < nFold; i++) {
			List train = (List) trainingData.get(i);
			List test = (List) testData.get(i);

			ranker = this.rFact.createRanker(this.type, train, features);
			ranker.set(this.trainScorer);
			ranker.init();
			ranker.learn();

			double s1 = evaluate(null, test);
			origScore += s1;

			double s2 = evaluate(ranker, test);
			rankScore += s2;

			double s3 = evaluate(null, createOracles(test));
			oracleScore += s3;
		}

		System.out.println(new StringBuilder().append("Total: ").append(SimpleMath.round(origScore / nFold, 4))
				.append("\t").append(SimpleMath.round(rankScore / nFold, 4)).append("\t")
				.append(SimpleMath.round(oracleScore / nFold, 4)).append("\t").toString());
	}

	public void test(String testFile) {
		List test = readInput(testFile);
		double rankScore = evaluate(null, test);
		System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
				.append(SimpleMath.round(rankScore, 4)).toString());
	}

	public void test(String modelFile, String testFile) {
		Ranker ranker = this.rFact.loadRanker(modelFile);
		int[] features = ranker.getFeatures();
		List test = readInput(testFile);
		if (normalize) {
			normalize(test, features);
		}
		double rankScore = evaluate(ranker, test);
		System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
				.append(SimpleMath.round(rankScore, 4)).toString());
	}

	public void test(String modelFile, String testFile, boolean printIndividual) {
		Ranker ranker = this.rFact.loadRanker(modelFile);
		int[] features = ranker.getFeatures();
		List test = readInput(testFile);
		if (normalize) {
			normalize(test, features);
		}
		double rankScore = 0.0D;
		double score = 0.0D;
		for (int i = 0; i < test.size(); i++) {
			RankList l = ranker.rank((RankList) test.get(i));
			score = this.testScorer.score(l);
			if (printIndividual)
				System.out.println(new StringBuilder().append(this.testScorer.name()).append("   ").append(l.getID())
						.append("   ").append(SimpleMath.round(score, 4)).toString());
			rankScore += score;
		}
		rankScore /= test.size();
		if (printIndividual)
			System.out.println(new StringBuilder().append(this.testScorer.name()).append("   all   ")
					.append(SimpleMath.round(rankScore, 4)).toString());
		else
			System.out.println(new StringBuilder().append(this.testScorer.name()).append(" on test data: ")
					.append(SimpleMath.round(rankScore, 4)).toString());
	}

	public void score(String modelFile, String testFile, String outputFile) {
		Ranker ranker = this.rFact.loadRanker(modelFile);
		int[] features = ranker.getFeatures();
		List test = readInput(testFile);
		if (normalize)
			normalize(test, features);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "ASCII"));
			for (int i = 0; i < test.size(); i++) {
				RankList l = (RankList) test.get(i);
				for (int j = 0; j < l.size(); j++) {
					out.write(new StringBuilder().append(ranker.eval(l.get(j))).append("").toString());
					out.newLine();
				}
			}
			out.close();
		} catch (Exception ex) {
			System.out.println(
					new StringBuilder().append("Error in Evaluator::rank(): ").append(ex.toString()).toString());
		}
	}

	public void rank(String modelFile, String testFile) {
		Ranker ranker = this.rFact.loadRanker(modelFile);
		int[] features = ranker.getFeatures();
		List test = readInput(testFile);
		if (normalize) {
			normalize(test, features);
		}
		for (int i = 0; i < test.size(); i++) {
			RankList l = (RankList) test.get(i);
			double[] scores = new double[l.size()];
			for (int j = 0; j < l.size(); j++)
				scores[j] = ranker.eval(l.get(j));
			int[] idx = Sorter.sort(scores, false);
			List ll = new ArrayList();
			for (int j = 0; j < idx.length; j++)
				ll.add(Integer.valueOf(idx[j]));
			for (int j = 0; j < l.size(); j++) {
				int index = ll.indexOf(Integer.valueOf(j)) + 1;
				System.out.print(new StringBuilder().append(index).append(j == l.size() - 1 ? "" : " ").toString());
			}
			System.out.println("");
		}
	}

	public void rank(String modelFile, String testFile, String indriRanking) {
		Ranker ranker = this.rFact.loadRanker(modelFile);
		int[] features = ranker.getFeatures();
		List test = readInput(testFile);
		if (normalize)
			normalize(test, features);
		try {
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(indriRanking), "ASCII"));
			for (int i = 0; i < test.size(); i++) {
				RankList l = (RankList) test.get(i);
				double[] scores = new double[l.size()];
				for (int j = 0; j < l.size(); j++)
					scores[j] = ranker.eval(l.get(j));
				int[] idx = MergeSorter.sort(scores, false);
				for (int j = 0; j < idx.length; j++) {
					int k = idx[j];
					String str = new StringBuilder().append(l.getID()).append(" Q0 ")
							.append(l.get(k).getDescription().replace("#", "").trim()).append(" ").append(j + 1)
							.append(" ").append(SimpleMath.round(scores[k], 5)).append(" indri").toString();
					out.write(str);
					out.newLine();
				}
			}
			out.close();
		} catch (Exception ex) {
			System.out.println(
					new StringBuilder().append("Error in Evaluator::rank(): ").append(ex.toString()).toString());
		}
	}

	private int[] prepareCV(String sampleFile, String featureDefFile, int nFold, boolean normalize,
			List<List<RankList>> trainingData, List<List<RankList>> testData) {
		List data = readInput(sampleFile);
		int[] features = readFeature(featureDefFile);
		if (features == null) {
			features = getFeatureFromSampleVector(data);
		}
		if (normalize)
			normalize(data, features);
		if (newFeatureFile.compareTo("") != 0) {
			System.out.print("Loading new feature description file... ");
			List descriptions = FileUtils.readLine(newFeatureFile, "ASCII");
			for (int i = 0; i < descriptions.size(); i++) {
				if (((String) descriptions.get(i)).indexOf("##") == 0)
					continue;
				LinearComputer lc = new LinearComputer("", (String) descriptions.get(i));

				if ((!keepOrigFeatures) || (lc.size() > 1))
					this.lcList.add(lc);
			}
			features = applyNewFeatures(data, features);
			System.out.println("[Done]");
		}

		List trainSamplesIdx = new ArrayList();
		int size = data.size() / nFold;
		int start = 0;
		int total = 0;
		for (int f = 0; f < nFold; f++) {
			List t = new ArrayList();
			for (int i = 0; (i < size) && (start + i < data.size()); i++)
				t.add(Integer.valueOf(start + i));
			trainSamplesIdx.add(t);
			total += t.size();
			start += size;
		}
		for (; total < data.size(); total++) {
			((List) trainSamplesIdx.get(trainSamplesIdx.size() - 1)).add(Integer.valueOf(total));
		}
		for (int i = 0; i < trainSamplesIdx.size(); i++) {
			List train = new ArrayList();
			List test = new ArrayList();

			List t = (List) trainSamplesIdx.get(i);
			for (int j = 0; j < data.size(); j++) {
				if (t.contains(Integer.valueOf(j)))
					test.add(new RankList((RankList) data.get(j)));
				else {
					train.add(new RankList((RankList) data.get(j)));
				}
			}
			trainingData.add(train);
			testData.add(test);
		}

		return features;
	}

	private int[] prepareSplit(String sampleFile, String featureDefFile, double percentTrain, boolean normalize,
			List<RankList> trainingData, List<RankList> testData) {
		List data = readInput(sampleFile);
		int[] features = readFeature(featureDefFile);
		if (features == null) {
			features = getFeatureFromSampleVector(data);
		}
		if (normalize)
			normalize(data, features);
		if (newFeatureFile.compareTo("") != 0) {
			System.out.print("Loading new feature description file... ");
			List descriptions = FileUtils.readLine(newFeatureFile, "ASCII");
			for (int i = 0; i < descriptions.size(); i++) {
				if (((String) descriptions.get(i)).indexOf("##") == 0)
					continue;
				LinearComputer lc = new LinearComputer("", (String) descriptions.get(i));

				if ((!keepOrigFeatures) || (lc.size() > 1))
					this.lcList.add(lc);
			}
			features = applyNewFeatures(data, features);
			System.out.println("[Done]");
		}

		int size = (int) (data.size() * percentTrain);

		for (int i = 0; i < size; i++)
			trainingData.add(new RankList((RankList) data.get(i)));
		for (int i = size; i < data.size(); i++) {
			testData.add(new RankList((RankList) data.get(i)));
		}
		return features;
	}

	private List<RankList> createOracles(List<RankList> rl) {
		List oracles = new ArrayList();
		for (int i = 0; i < rl.size(); i++) {
			oracles.add(((RankList) rl.get(i)).getCorrectRanking());
		}
		return oracles;
	}

	public int[] getFeatureFromSampleVector(List<RankList> samples) {
		DataPoint dp = ((RankList) samples.get(0)).get(0);
		int fc = dp.getFeatureCount();
		int[] features = new int[fc];
		for (int i = 0; i < fc; i++)
			features[i] = (i + 1);
		return features;
	}

	private int[] applyNewFeatures(List<RankList> samples, int[] features) {
		int totalFeatureCount = ((RankList) samples.get(0)).get(0).getFeatureCount();
		int[] newFeatures = new int[features.length + this.lcList.size()];
		System.arraycopy(features, 0, newFeatures, 0, features.length);

		for (int k = 0; k < this.lcList.size(); k++) {
			newFeatures[(features.length + k)] = (totalFeatureCount + k + 1);
		}
		float[] addedFeatures = new float[this.lcList.size()];
		for (int i = 0; i < samples.size(); i++) {
			RankList rl = (RankList) samples.get(i);
			for (int j = 0; j < rl.size(); j++) {
				DataPoint p = rl.get(j);
				for (int k = 0; k < this.lcList.size(); k++) {
					addedFeatures[k] = ((LinearComputer) this.lcList.get(k)).compute(p.getExternalFeatureVector());
				}
				p.addFeatures(addedFeatures);
			}
		}

		int[] newFeatures2 = new int[this.lcList.size()];
		for (int i = 0; i < this.lcList.size(); i++) {
			newFeatures2[i] = newFeatures[(i + features.length)];
		}
		if (keepOrigFeatures)
			return newFeatures;
		return newFeatures2;
	}
}