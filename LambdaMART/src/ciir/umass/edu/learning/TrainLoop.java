package ciir.umass.edu.learning;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class TrainLoop {
	private String trainFile;
	private String validationFile;
	private String testFile;
	private String featureDescriptionFile;
	private String savedModelFile;
	private String onlineModel;
	private String outputPath;
	private String outputScore;
	private String metric2t;
	private String metric2T;
	private String featureMinAndMaxFile;
	private int minLeaf;
	private int maxLeaf;
	private int step;
	DecimalFormat df = new DecimalFormat("0.0000");

	public TrainLoop(String trainFile, String validationFile, String testFile, String featureDescriptionFile,
			String savedModelFile, String onlineModel, String outputPath, int minLeaf, int maxLeaf, int step,
			String metric2t, String metric2T, String outputScore, String featureMinAndMaxFile) {
		this.trainFile = trainFile;
		this.validationFile = validationFile;
		this.testFile = testFile;
		this.featureDescriptionFile = featureDescriptionFile;
		this.savedModelFile = savedModelFile;
		this.onlineModel = onlineModel;
		this.outputPath = outputPath;
		this.outputScore = outputScore;
		this.metric2t = metric2t;
		this.metric2T = metric2T;
		this.minLeaf = minLeaf;
		this.maxLeaf = maxLeaf;
		this.step = step;
		this.featureMinAndMaxFile = featureMinAndMaxFile;
	}

	public void work() {
		System.out.println("work begin");
		PrintWriter printWriter = null;
		try {
			printWriter = new PrintWriter(new FileOutputStream(this.outputPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		Node node = new Node(this.featureDescriptionFile);
		Trainer initTrainer = new Trainer(this.onlineModel, this.testFile, this.metric2T);
		double scoreOnline = 0.0D;

		String[] ComputeScoreTypes = { "ERR3", "WithNotLabel", "WithoutNotLabel" };
		double[] scoreOnlines = { 0.0D, 0.0D, 0.0D };
		for (int s = 0; s < ComputeScoreTypes.length; s++) {
			scoreOnline = initTrainer.score();
			scoreOnlines[s] = scoreOnline;
			printWriter.println("scoreOnline:" + ComputeScoreTypes[s] + " " + scoreOnline);
		}
		initTrainer = null;
		Trainer trainer = new Trainer(this.trainFile, this.validationFile, this.testFile, this.savedModelFile,
				this.onlineModel, this.metric2t, this.metric2T);

		System.out.println("search start");

		for (int i = this.minLeaf; i <= this.maxLeaf; i += this.step) {
			System.out.println("leaf@" + i);
			ciir.umass.edu.learning.tree.LambdaMART.nTreeLeaves = i;
			trainer.train(node);
			printWriter.println("scoreOnTrainingData " + i + " : " + df.format(trainer.trainScore));
			double score = 0.0D;
			double winRatio = 0.0D;
			for (int s = 0; s < ComputeScoreTypes.length; s++) {
				score = trainer.score();
				winRatio = trainer.scoreWin(ComputeScoreTypes[s]);
				printWriter.println("score@leaf " + i + " " + ComputeScoreTypes[s] + ": " + df.format(score));
				printWriter.println("winRatio@leaf " + i + " " + ComputeScoreTypes[s] + ": " + df.format(winRatio));
			}

			if ((score > scoreOnline) && (winRatio > 0.5D)) {
				System.out.println("@winRatioERR@" + df.format(score) + " > " + df.format(scoreOnline) + 
						"@winRatio" + df.format(winRatio) + "@leaf" + i);
				trainer.save("winRatioERR." + i);
			} else if (score > scoreOnline) {
				System.out.println(df.format(score) + " > " + df.format(scoreOnline) + "@" + i);
				trainer.save("ERR." + i);
			} else if (winRatio > 0.5D) {
				System.out.println(df.format(score) + " > " + df.format(scoreOnline) + "@" + i);
				trainer.save("winRatio." + i);
			} else {
				System.out.println(df.format(score) + " < " + df.format(scoreOnline) + "@" + i);
				trainer.save("" + i);
			}
		}
		System.out.println("search end");
		printWriter.close();
		System.out.println("work end");
	}
}