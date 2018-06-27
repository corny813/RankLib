package ciir.umass.edu.learning;

import java.io.PrintStream;

public class FeatureSelect {
	public static void compoundSearch() {
		System.out.println("compound search");

		String dir = "label/";
		String trainFile = dir + "train.label";
		String validationFile = dir + "test.label";
		String testFile = dir + "valid.label";

		String modelFile = dir + "fs.model";
		String onlineModel = dir + "relevance.model";

		Trainer trainer = new Trainer(trainFile, validationFile, testFile, modelFile);
		Trainer initTrainer = new Trainer(onlineModel, testFile);

		String initFeatureFile = dir + "feature.online";
		String allFeatureFile = dir + "feature.online";
		CompoundSearch compoundSearch = new CompoundSearch(initFeatureFile, allFeatureFile, initTrainer, trainer);
		compoundSearch.work();
	}
}