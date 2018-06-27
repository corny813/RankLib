package ciir.umass.edu.learning;

import java.io.PrintStream;

public class AutoTrain {
	public static void loopAuto() {
		System.out.println("auto loop");
		String dir = "label/";
		String trainFile = dir + "all.label.5.2m";

		String validationFile = dir + "valid.label";
		String testFile = dir + "test.label";

		String onlineModel = dir + "auto.model";
		Trainer initTrainer = new Trainer(onlineModel, testFile);
		double scoreOnline = initTrainer.score();

		String modelFile = dir + "model.2m.vt";
		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);

		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 300;
		ciir.umass.edu.learning.tree.LambdaMART.nRoundToStopEarly = 100;
		ciir.umass.edu.learning.tree.LambdaMART.minLeafSupport = 100;

		int minLeaf = 10;
		int maxLeaf = 50;
		int step = 5;
		for (int i = minLeaf; i <= maxLeaf; i += step) {
			System.err.println("loop@" + i);
			ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = i;
			trainer.train(node);
			double score = trainer.score();
			if (score > scoreOnline) {
				System.err.println(score + ">" + scoreOnline + "@" + i);
				trainer.save(i);
			}
		}
	}

	public static void auto() {
		System.out.println("auto train");
		String dir = "label/";
		String trainFile = dir + "all.label.5.1m";

		String validationFile = dir + "test.label";
		String testFile = dir + "valid.label";

		String onlineModel = dir + "auto.model";
		Trainer initTrainer = new Trainer(onlineModel, testFile);
		double scoreOnline = initTrainer.score();
		System.out.println("online:" + scoreOnline);

		String modelFile = dir + "auto.model";
		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);

		String featurePath = dir + "feature.online";
		Node node = new Node(featurePath);
		ciir.umass.edu.learning.tree.LambdaMART.nTrees = 250;
		ciir.umass.edu.learning.tree.LambdaMART.nRoundToStopEarly = 50;
		ciir.umass.edu.learning.tree.LambdaMART.minLeafSupport = 100;
		ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = 10;
		trainer.train(node);
		double score = trainer.score();
		System.out.println("auto:" + score);
		trainer.save(0);
	}
}