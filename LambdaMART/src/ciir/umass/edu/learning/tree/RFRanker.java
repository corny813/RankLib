package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.Sampler;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class RFRanker extends Ranker {
	public static int nBag = 300;
	public static float subSamplingRate = 1.0F;
	public static float featureSamplingRate = 0.3F;

	public static RANKER_TYPE rType = RANKER_TYPE.MART;
	public static int nTrees = 1;
	public static int nTreeLeaves = 100;
	public static float learningRate = 0.1F;

	public static int nThreshold = 256;
	public static int minLeafSupport = 1;

	protected Ensemble[] ensembles = null;

	public RFRanker() {
	}

	public RFRanker(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	public void init() {
		PRINT("Initializing... ");
		this.ensembles = new Ensemble[nBag];

		LambdaMART.nTrees = nTrees;
		LambdaMART.nTreeLeaves = nTreeLeaves;
		LambdaMART.learningRate = learningRate;
		LambdaMART.nThreshold = nThreshold;
		LambdaMART.minLeafSupport = minLeafSupport;
		LambdaMART.nRoundToStopEarly = -1;

		FeatureHistogram.samplingRate = featureSamplingRate;
		PRINTLN("[Done]");
	}

	public void learn() {
		RankerFactory rf = new RankerFactory();
		PRINTLN("------------------------------------");
		PRINTLN("Training starts...");
		PRINTLN("------------------------------------");
		PRINTLN(new int[] { 9, 9, 11 }, new String[] { "bag", this.scorer.name() + "-B", this.scorer.name() + "-OOB" });
		PRINTLN("------------------------------------");

		for (int i = 0; i < nBag; i++) {
			System.gc();
			Sampler sp = new Sampler();

			List bag = sp.doSampling(this.samples, subSamplingRate, true);

			LambdaMART r = (LambdaMART) rf.createRanker(rType, bag, this.features);

			Ranker.verbose = false;
			r.init();
			r.set(this.scorer);
			r.learn();
			Ranker.verbose = true;

			PRINTLN(new int[] { 9, 9 },
					new String[] { "b[" + (i + 1) + "]", SimpleMath.round(r.getScoreOnTrainingData(), 4) + "" });
			this.ensembles[i] = r.getEnsemble();
		}

		this.scoreOnTrainingData = this.scorer.score(rank(this.samples));
		PRINTLN("------------------------------------");
		PRINTLN("Finished sucessfully.");
		PRINTLN(this.scorer.name() + " on training data: " + SimpleMath.round(this.scoreOnTrainingData, 4));
		if (this.validationSamples != null) {
			this.bestScoreOnValidationData = this.scorer.score(rank(this.validationSamples));
			PRINTLN(this.scorer.name() + " on validation data: " + SimpleMath.round(this.bestScoreOnValidationData, 4));
		}
		PRINTLN("------------------------------------");
	}

	public double eval(DataPoint dp) {
		double s = 0.0D;
		for (int i = 0; i < this.ensembles.length; i++)
			s += this.ensembles[i].eval(dp);
		return s / this.ensembles.length;
	}

	public Ranker clone() {
		return new RFRanker();
	}

	public String toString() {
		String str = "";
		for (int i = 0; i < nBag; i++)
			str = str + this.ensembles[i].toString() + "\n";
		return str;
	}

	public String model() {
		String output = "## " + name() + "\n";
		output = output + "## No. of bags = " + nBag + "\n";
		output = output + "## Sub-sampling = " + subSamplingRate + "\n";
		output = output + "## Feature-sampling = " + featureSamplingRate + "\n";
		output = output + "## No. of trees = " + nTrees + "\n";
		output = output + "## No. of leaves = " + nTreeLeaves + "\n";
		output = output + "## No. of threshold candidates = " + nThreshold + "\n";
		output = output + "## Learning rate = " + learningRate + "\n";
		output = output + "\n";
		output = output + toString();
		return output;
	}

	public void load(String fn) {
		try {
			String content = "";
			String model = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));
			List ens = new ArrayList();
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("##") == 0)) {
					continue;
				}
				model = model + content;
				if (content.indexOf("</ensemble>") == -1) {
					continue;
				}
				ens.add(new Ensemble(model));
				model = "";
			}

			in.close();
			this.ensembles = new Ensemble[ens.size()];
			for (int i = 0; i < ens.size(); i++)
				this.ensembles[i] = ((Ensemble) ens.get(i));
		} catch (Exception ex) {
			System.out.println("Error in RFRanker::load(): " + ex.toString());
		}
	}

	public void printParameters() {
		PRINTLN("No. of bags: " + nBag);
		PRINTLN("Sub-sampling: " + subSamplingRate);
		PRINTLN("Feature-sampling: " + featureSamplingRate);
		PRINTLN("No. of trees: " + nTrees);
		PRINTLN("No. of leaves: " + nTreeLeaves);
		PRINTLN("No. of threshold candidates: " + nThreshold);
		PRINTLN("Learning rate: " + learningRate);
	}

	public String name() {
		return "Random Forests";
	}

	public Ensemble[] getEnsembles() {
		return this.ensembles;
	}
}