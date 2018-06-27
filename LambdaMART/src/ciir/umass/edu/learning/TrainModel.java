package ciir.umass.edu.learning;

import ciir.umass.edu.features.FeatureManager;
import java.io.PrintStream;
import java.util.List;

public class TrainModel {
	public static boolean letor = false;
	public static boolean mustHaveRelDoc = false;

	public static void trainModel() {
		System.out.println("train model");

		String dir = "1031.auto/";

		String trainFile = dir + "label.1031";

		System.err.println(trainFile);
		Trainer trainer = new Trainer(trainFile);

		String featurePath = dir + "featureid.online";

		System.out.println(featurePath);

		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 1;

		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 50;

		ciir.umass.edu.learning.tree.LambdaMART.modelDir = dir + "m.5m/";
		trainer.train(node);
	}

	public static void trainModelx() {
		System.out.println("train model");
		String dir = "kkk/";
		String trainFile = dir + "test.label";
		System.err.println(trainFile);
		Trainer trainer = new Trainer(trainFile);
		String featurePath = dir + "featureid.online";

		System.out.println(featurePath);

		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 10;

		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 30;

		ciir.umass.edu.learning.tree.LambdaMART.modelDir = "kkk/";
		trainer.train(node);
	}

	public static void trainRanknet() {
		System.out.println("train model");
		String dir = "717/";

		String trainFile = dir + "test.label";

		System.err.println(trainFile);
		Trainer trainer = new Trainer(trainFile);

		String featurePath = dir + "featureid.online";

		System.out.println(featurePath);

		Node node = new Node(featurePath);

		trainer.trainRanknet(node);
	}

	public static void trainModelTest() {
		System.out.println("train model");
		String dir = "717/";

		String trainFile = dir + "label.10w";
		System.err.println(trainFile);
		Trainer trainer = new Trainer(trainFile);

		String featurePath = dir + "feature.me";
		System.out.println(featurePath);
		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 300;
		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 30;
		trainer.train(node);
	}

	public static void trainModel2() {
		String dir = "717/";
		String trainFile = dir + "0.label.match.5.01.f";

		System.err.println(trainFile);
		Trainer trainer = new Trainer(trainFile);
		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 400;

		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 30;
		trainer.train(node);

		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 50;
		trainer.train(node);
	}

	public static List<RankList> readInput(String inputFile) {
		FeatureManager fm = new FeatureManager();
		List samples = fm.read(inputFile, letor, mustHaveRelDoc);
		return samples;
	}

	public static void testModel() {
		String dir = "./";
		String dir2 = "5m/";
		int leaf = 50;

		String testFile = dir + "3k.label";
		List test = readInput(testFile);
		for (int i = 25; i <= 300; i += 25) {
			String modelFile = dir2 + "m." + leaf + "." + i;
			Trainer initTrainer = new Trainer(modelFile, test);
			System.out.println(i);
			initTrainer.score();
		}
	}

	public static void testModelCombineNew(String dir, int leaf, String testName, String modelName) {
		System.out.println(leaf);

		String testFile = dir + testName;

		System.out.println(testFile);
		List test = readInput(testFile);
		for (int i = 50; i <= 200; i += 10) {
			System.out.println(i);

			String modelFile = dir + modelName;
			System.out.println(modelFile);
			String modelFile2 = dir + "m." + leaf + "." + i;
			System.out.println(modelFile2);
			Trainer initTrainer = new Trainer(modelFile, modelFile2, test);

			double max = 4.9E-324D;
			int index = 10;
			for (int j = 0; j <= 10; j++) {
				System.out.println("@@@" + j);
				double cof = j * 0.1D;
				double v = initTrainer.score2(cof);
				if (v > max) {
					max = v;
					index = j;
				}
			}
			System.out.println("###" + index + "@" + max);
		}
	}

	public static void searchCombine(String modelFile, String modelFile2, String testFile) {
		System.out.println(testFile);
		System.out.println(modelFile);
		System.out.println(modelFile2);
		List test = readInput(testFile);
		Trainer initTrainer = new Trainer(modelFile2, modelFile, test);
		double max = 4.9E-324D;
		int index = 10;
		for (int j = 0; j <= 10; j++) {
			System.out.println("@@@" + j);
			double cof = j * 0.1D;
			double v = initTrainer.score2(cof);
			if (v > max) {
				max = v;
				index = j;
			}
		}
		System.out.println();
		System.out.println("BestCof: " + index * 0.1D);
		System.out.println("MaxERR: " + max);
	}

	public static void testModelCombine(int leaf) {
		System.out.println(leaf);

		String dir = "904.auto/";
		String dir2 = "904.auto.5m/";

		String testFile = dir + "ltr.validate";

		System.out.println(testFile);
		List test = readInput(testFile);
		for (int i = 100; i <= 200; i += 10) {
			System.out.println(i);

			String modelFile = dir + "relevance.model.mergefield";
			String modelFile2 = dir2 + "m." + leaf + "." + i;
			System.out.println(modelFile2);
			Trainer initTrainer = new Trainer(modelFile, modelFile2, test);
			double max = 4.9E-324D;
			int index = 10;
			for (int j = 0; j <= 10; j++) {
				System.out.println("@@@" + j);
				double cof = j * 0.1D;
				double v = initTrainer.score2(cof);
				if (v > max) {
					max = v;
					index = j;
				}
			}
			System.out.println("###" + index + "@" + max);
		}
	}

	public static void testModelCombineOne(int leaf, int tree) {
		System.out.println(leaf + "\t" + tree);
		String dir = "tested.model/";
		String dir2 = "tested.model/";
		String testFile = dir + "test.label";
		System.out.println(testFile);
		List test = readInput(testFile);
		String modelFile = dir + "relevance.model";
		String modelFile2 = dir2 + "m." + leaf + "." + tree;
		System.out.println(modelFile2);
		Trainer initTrainer = new Trainer(modelFile, modelFile2, test);
		for (int j = 0; j <= 10; j++) {
			System.out.println("@@@" + j);
			double cof = j * 0.1D;
			initTrainer.score2(cof);
		}
	}

	public static void testModel3() {
		String dir = "now/";

		String dir2 = "now/m.0/";

		String testFile = dir + "test3.label";
		List test = readInput(testFile);

		for (int i = 25; i <= 75; i += 25) {
			System.out.println("kkk");
			String modelFile = dir2 + "m.50." + i;
			Trainer initTrainer = new Trainer(modelFile, test);
			System.out.println(i);
			initTrainer.score();
		}
	}

	public static void testModelOnline() {
		String dir = "./";
		String onlineModel = dir + "SearchLambdaMART.model.f.38";

		String testFile = dir + "3k.label";
		Trainer initTrainer = new Trainer(onlineModel, testFile);
		initTrainer.score();
	}

	public static void trainValid() {
		System.out.println("train valid");

		String dir = "now/";
		String trainFile = dir + "train.label.new";

		String validationFile = dir + "test.label.new";
		String testFile = dir + "valid.label.new";

		String modelFile = dir + "model.rel";

		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);

		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);
		trainer.train(node);
		trainer.score();
		trainer.save(57);
	}

	public static void loop() {
		System.out.println("train kkk");
		String dir = "now/";
		String trainFile = dir + "train.label.new";
		String validationFile = dir + "valid.label.new";
		String testFile = dir + "test.label.new";

		String onlineModel = dir + "relevance.model.online";
		Trainer initTrainer = new Trainer(onlineModel, testFile);
		double scoreOnline = initTrainer.score();

		String modelFile = dir + "model.rel";
		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);

		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);

		int minLeaf = 30;
		int maxLeaf = 50;

		int step = 10;

		for (int i = maxLeaf; i >= minLeaf; i -= step) {
			System.err.println("loop@" + i);
			ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = i;
			trainer.train(node);
			double score = trainer.score();
			if (score > scoreOnline) {
				System.err.println(score + ">" + scoreOnline + "@" + i);
			}

			trainer.save(i);
		}
	}

	public static void loopAuto() {
		System.out.println("train auto.label.7m");
		String dir = "label/";

		String trainFile = dir + "auto.all.label";

		String validationFile = dir + "test.label";
		String testFile = dir + "valid.label";

		String onlineModel = dir + "auto.model";
		Trainer initTrainer = new Trainer(onlineModel, testFile);
		double scoreOnline = initTrainer.score();

		String modelFile = dir + "model.auto.label.7m.tv";

		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);

		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);

		int minLeaf = 10;
		int maxLeaf = 50;

		int step = 10;

		for (int i = minLeaf; i <= maxLeaf; i += step) {
			System.err.println("loop@" + i);
			ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = i;
			ciir.umass.edu.learning.tree.LambdaMART.nRoundToStopEarly = 100;
			trainer.train(node);
			double score = trainer.score();
			if (score > scoreOnline) {
				System.err.println(score + ">" + scoreOnline + "@" + i);
			}

			trainer.save(i);
		}
	}
}