package ciir.umass.edu.learning;

import java.io.PrintStream;

public class Main {
	public static void main(String[] args) {
		search(args);
	}

	private static void searchCombine(String[] args) {
		if (args.length != 3) {
			System.out.println("usage:");
			System.out.println("java -jar SearchCombine.jar RelevanceModel AutoModel TestFile");
			System.exit(-1);
		}
		String modelPath = args[0];
		String modelPath2 = args[1];
		String testPath = args[2];
		TrainModel.searchCombine(modelPath, modelPath2, testPath);
	}

	public static void search(String[] args) {
		String path = "data/";

		String featureDescriptionFile = path + "featureid.online";

		String trainFile = path + "trainSet";

		String validationFile = path + "testSet";

		String testFile = path + "validSet";

		String savedModelFile = path + "test_modify_model";
		String onlineModel = path + "base_model";
		String outputPath = path + "output/model.f.result";
		String outputScore = path + "relevance.model.dump.score";
		String featureMinAndMaxFile = "";
		int minLeaf = 20;
		int maxLeaf = 20;
		int step = 1;
		String metric2t = "DCG@5";
		String metric2T = "ERR@5";
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 10;

		for (int i = 0; i < args.length; i++) {
			if (args[i].compareTo("-train") == 0) {
				i++;
				trainFile = args[i];
			} else if (args[i].compareTo("-feature") == 0) {
				i++;
				featureDescriptionFile = args[i];
			} else if (args[i].compareTo("-validate") == 0) {
				i++;
				validationFile = args[i];
			} else if (args[i].compareTo("-test") == 0) {
				i++;
				testFile = args[i];
			} else if (args[i].compareTo("-save") == 0) {
				i++;
				savedModelFile = args[i];
			} else if (args[i].compareTo("-baselineModel") == 0) {
				i++;
				onlineModel = args[i];
			} else if (args[i].compareTo("-result") == 0) {
				i++;
				outputPath = args[i];
			} else if (args[i].compareTo("-metric2t") == 0) {
				i++;
				metric2t = args[i];
			} else if (args[i].compareTo("-metric2T") == 0) {
				i++;
				metric2T = args[i];
			} else if (args[i].compareTo("-resultScore") == 0) {
				i++;
				outputScore = args[i];
			} else if (args[i].compareTo("-nThreshold") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.nThreshold = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-FeatureMinAndMaxValue") == 0) {
				i++;
				featureMinAndMaxFile = args[i];
				ciir.umass.edu.learning.tree.LambdaMART.featureMinAndMaxFile = featureMinAndMaxFile;
			} else if (args[i].compareTo("-maxTree") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.nTrees = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-minLeaf") == 0) {
				i++;
				minLeaf = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-maxLeaf") == 0) {
				i++;
				maxLeaf = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-stepLeaf") == 0) {
				i++;
				step = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-shrinkage") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.learningRate = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-mls") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.minLeafSupport = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-estop") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.nRoundToStopEarly = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-loopMax") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.loopMax = Integer.parseInt(args[i]);
			} else if (args[i].compareTo("-clickDecreseWeight") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.clickDecreseWeight = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-lambdaPower") == 0) {
				i++;
				ciir.umass.edu.metric.DCGScorer.basiclog = Float.parseFloat(args[i]);
			} else if (args[i].compareTo("-samplingFeature") == 0) {
				i++;
				ciir.umass.edu.learning.tree.LambdaMART.samplingFeature = Float.parseFloat(args[i]);
			}

		}

		TrainLoop trainLoop = new TrainLoop(trainFile, validationFile, testFile, featureDescriptionFile, savedModelFile,
				onlineModel, outputPath, minLeaf, maxLeaf, step, metric2t, metric2T, outputScore, featureMinAndMaxFile);
		trainLoop.work();
	}

	private static void test() {
		System.out.println("MART");

		TrainModel.trainModelx();

		int leaf = 50;
		String dir = "";
		String testName = "";
		String modelName = "";

		dir = "1012.m/";

		testName = "ltr.test.3";
	}
}