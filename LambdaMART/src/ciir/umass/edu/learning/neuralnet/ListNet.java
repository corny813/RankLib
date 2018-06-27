package ciir.umass.edu.learning.neuralnet;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.SimpleMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ListNet extends RankNet {
	public static int nIteration = 1500;
	public static double learningRate = 1.E-005D;
	public static int nHiddenLayer = 0;

	public ListNet() {
	}

	public ListNet(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	protected float[] feedForward(RankList rl) {
		float[] labels = new float[rl.size()];
		for (int i = 0; i < rl.size(); i++) {
			addInput(rl.get(i));
			propagate(i);
			labels[i] = rl.get(i).getLabel();
		}
		return labels;
	}

	protected void backPropagate(float[] labels) {
		PropParameter p = new PropParameter(labels);
		this.outputLayer.computeDelta(p);

		this.outputLayer.updateWeight(p);
	}

	protected void estimateLoss() {
		this.error = 0.0D;
		double sumLabelExp = 0.0D;
		double sumScoreExp = 0.0D;
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = (RankList) this.samples.get(i);
			double[] scores = new double[rl.size()];
			double err = 0.0D;
			for (int j = 0; j < rl.size(); j++) {
				scores[j] = eval(rl.get(j));
				sumLabelExp += Math.exp(rl.get(j).getLabel());
				sumScoreExp += Math.exp(scores[j]);
			}
			for (int j = 0; j < rl.size(); j++) {
				double p1 = Math.exp(rl.get(j).getLabel()) / sumLabelExp;
				double p2 = Math.exp(scores[j]) / sumScoreExp;
				err += -p1 * SimpleMath.logBase2(p2);
			}
			this.error += err / rl.size();
		}

		this.lastError = this.error;
	}

	public void init() {
		PRINT("Initializing... ");

		setInputOutput(this.features.length, 1, 1);
		wire();

		if (this.validationSamples != null) {
			for (int i = 0; i < this.layers.size(); i++)
				this.bestModelOnValidation.add(new ArrayList());
		}
		Neuron.learningRate = learningRate;
		PRINTLN("[Done]");
	}

	public void learn() {
		PRINTLN("-----------------------------------------");
		PRINTLN("Training starts...");
		PRINTLN("--------------------------------------------------");
		PRINTLN(new int[] { 7, 14, 9, 9 },
				new String[] { "#epoch", "C.E. Loss",
						new StringBuilder().append(this.scorer.name()).append("-T").toString(),
						new StringBuilder().append(this.scorer.name()).append("-V").toString() });
		PRINTLN("--------------------------------------------------");

		for (int i = 1; i <= nIteration; i++) {
			for (int j = 0; j < this.samples.size(); j++) {
				float[] labels = feedForward((RankList) this.samples.get(j));
				backPropagate(labels);
				clearNeuronOutputs();
			}

			PRINT(new int[] { 7, 14 }, new String[] { new StringBuilder().append(i).append("").toString(),
					new StringBuilder().append(SimpleMath.round(this.error, 6)).append("").toString() });
			if (i % 1 == 0) {
				this.scoreOnTrainingData = this.scorer.score(rank(this.samples));
				PRINT(new int[] { 9 }, new String[] { new StringBuilder()
						.append(SimpleMath.round(this.scoreOnTrainingData, 4)).append("").toString() });
				if (this.validationSamples != null) {
					double score = this.scorer.score(rank(this.validationSamples));
					if (score > this.bestScoreOnValidationData) {
						this.bestScoreOnValidationData = score;
						saveBestModelOnValidation();
					}
					PRINT(new int[] { 9 }, new String[] {
							new StringBuilder().append(SimpleMath.round(score, 4)).append("").toString() });
				}
			}
			PRINTLN("");
		}

		if (this.validationSamples != null) {
			restoreBestModelOnValidation();
		}
		this.scoreOnTrainingData = SimpleMath.round(this.scorer.score(rank(this.samples)), 4);
		PRINTLN("--------------------------------------------------");
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
		return super.eval(p);
	}

	public Ranker clone() {
		return new ListNet();
	}

	public String toString() {
		return super.toString();
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();
		output = new StringBuilder().append(output).append("## Epochs = ").append(nIteration).append("\n").toString();
		output = new StringBuilder().append(output).append("## No. of features = ").append(this.features.length)
				.append("\n").toString();

		for (int i = 0; i < this.features.length; i++)
			output = new StringBuilder().append(output).append(this.features[i])
					.append(i == this.features.length - 1 ? "" : " ").toString();
		output = new StringBuilder().append(output).append("\n").toString();

		output = new StringBuilder().append(output).append("0\n").toString();

		output = new StringBuilder().append(output).append(toString()).toString();
		return output;
	}

	public void load(String fn) {
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));

			List l = new ArrayList();
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("##") == 0))
					continue;
				l.add(content);
			}
			in.close();

			String[] tmp = ((String) l.get(0)).split(" ");
			this.features = new int[tmp.length];
			for (int i = 0; i < tmp.length; i++) {
				this.features[i] = Integer.parseInt(tmp[i]);
			}
			int nHiddenLayer = Integer.parseInt((String) l.get(1));
			int[] nn = new int[nHiddenLayer];

			int i = 2;
			for (; i < 2 + nHiddenLayer; i++) {
				nn[(i - 2)] = Integer.parseInt((String) l.get(i));
			}
			setInputOutput(this.features.length, 1);
			for (int j = 0; j < nHiddenLayer; j++)
				addHiddenLayer(nn[j]);
			wire();

			for (; i < l.size(); i++) {
				String[] s = ((String) l.get(i)).split(" ");
				int iLayer = Integer.parseInt(s[0]);
				int iNeuron = Integer.parseInt(s[1]);
				Neuron n = ((Layer) this.layers.get(iLayer)).get(iNeuron);
				for (int k = 0; k < n.getOutLinks().size(); k++)
					((Synapse) n.getOutLinks().get(k)).setWeight(Double.parseDouble(s[(k + 2)]));
			}
		} catch (Exception ex) {
			System.out
					.println(new StringBuilder().append("Error in ListNet::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of epochs: ").append(nIteration).toString());
		PRINTLN(new StringBuilder().append("Learning rate: ").append(learningRate).toString());
	}

	public String name() {
		return "ListNet";
	}
}