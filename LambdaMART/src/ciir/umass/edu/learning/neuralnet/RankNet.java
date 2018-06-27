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

public class RankNet extends Ranker {
	public static int nIteration = 100;
	public static int nHiddenLayer = 1;
	public static int nHiddenNodePerLayer = 10;
	public static double learningRate = 5.E-005D;

	protected List<Layer> layers = new ArrayList();
	protected Layer inputLayer = null;
	protected Layer outputLayer = null;

	protected List<List<Double>> bestModelOnValidation = new ArrayList();

	protected int totalPairs = 0;
	protected int misorderedPairs = 0;
	protected double error = 0.0D;
	protected double lastError = 1.7976931348623157E+308D;
	protected int straightLoss = 0;

	public RankNet() {
	}

	public RankNet(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	protected void setInputOutput(int nInput, int nOutput) {
		this.inputLayer = new Layer(nInput + 1);
		this.outputLayer = new Layer(nOutput);
		this.layers.clear();
		this.layers.add(this.inputLayer);
		this.layers.add(this.outputLayer);
	}

	protected void setInputOutput(int nInput, int nOutput, int nType) {
		this.inputLayer = new Layer(nInput + 1, nType);
		this.outputLayer = new Layer(nOutput, nType);
		this.layers.clear();
		this.layers.add(this.inputLayer);
		this.layers.add(this.outputLayer);
	}

	protected void addHiddenLayer(int size) {
		this.layers.add(this.layers.size() - 1, new Layer(size));
	}

	protected void wire() {
		for (int i = 0; i < this.inputLayer.size() - 1; i++) {
			for (int j = 0; j < ((Layer) this.layers.get(1)).size(); j++) {
				connect(0, i, 1, j);
			}
		}
		for (int i = 1; i < this.layers.size() - 1; i++) {
			for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++) {
				for (int k = 0; k < ((Layer) this.layers.get(i + 1)).size(); k++)
					connect(i, j, i + 1, k);
			}
		}
		for (int i = 1; i < this.layers.size(); i++)
			for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++)
				connect(0, this.inputLayer.size() - 1, i, j);
	}

	protected void connect(int sourceLayer, int sourceNeuron, int targetLayer, int targetNeuron) {
		new Synapse(((Layer) this.layers.get(sourceLayer)).get(sourceNeuron),
				((Layer) this.layers.get(targetLayer)).get(targetNeuron));
	}

	protected void addInput(DataPoint p) {
		for (int k = 0; k < this.inputLayer.size() - 1; k++) {
			this.inputLayer.get(k).addOutput(p.getFeatureValue(this.features[k]));
		}
		this.inputLayer.get(this.inputLayer.size() - 1).addOutput(1.0D);
	}

	protected void propagate(int i) {
		for (int k = 1; k < this.layers.size(); k++)
			((Layer) this.layers.get(k)).computeOutput(i);
	}

	protected int[][] batchFeedForward(RankList rl) {
		int[][] pairMap = new int[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			addInput(rl.get(i));
			propagate(i);

			int count = 0;
			for (int j = 0; j < rl.size(); j++) {
				if (rl.get(i).getLabel() > rl.get(j).getLabel())
					count++;
			}
			pairMap[i] = new int[count];
			int k = 0;
			for (int j = 0; j < rl.size(); j++) {
				if (rl.get(i).getLabel() > rl.get(j).getLabel()) {
					pairMap[i][(k++)] = j;
				}

			}

		}

		return pairMap;
	}

	protected void batchBackPropagate(int[][] pairMap, float[][] pairWeight) {
		for (int i = 0; i < pairMap.length; i++) {
			PropParameter p = new PropParameter(i, pairMap);
			this.outputLayer.computeDelta(p);
			for (int j = this.layers.size() - 2; j >= 1; j--) {
				((Layer) this.layers.get(j)).updateDelta(p);
			}

			this.outputLayer.updateWeight(p);
			for (int j = this.layers.size() - 2; j >= 1; j--)
				((Layer) this.layers.get(j)).updateWeight(p);
		}
	}

	protected void clearNeuronOutputs() {
		for (int k = 0; k < this.layers.size(); k++)
			((Layer) this.layers.get(k)).clearOutputs();
	}

	protected float[][] computePairWeight(int[][] pairMap, RankList rl) {
		return (float[][]) null;
	}

	protected RankList internalReorder(RankList rl) {
		return rl;
	}

	protected void saveBestModelOnValidation() {
		for (int i = 0; i < this.layers.size() - 1; i++) {
			List l = (List) this.bestModelOnValidation.get(i);
			l.clear();
			for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++) {
				Neuron n = ((Layer) this.layers.get(i)).get(j);
				for (int k = 0; k < n.getOutLinks().size(); k++)
					l.add(Double.valueOf(((Synapse) n.getOutLinks().get(k)).getWeight()));
			}
		}
	}

	protected void restoreBestModelOnValidation() {
		try {
			for (int i = 0; i < this.layers.size() - 1; i++) {
				List l = (List) this.bestModelOnValidation.get(i);
				int c = 0;
				for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++) {
					Neuron n = ((Layer) this.layers.get(i)).get(j);
					for (int k = 0; k < n.getOutLinks().size(); k++)
						((Synapse) n.getOutLinks().get(k)).setWeight(((Double) l.get(c++)).doubleValue());
				}
			}
		} catch (Exception ex) {
			System.out.println(new StringBuilder().append("Error in NeuralNetwork.restoreBestModelOnValidation(): ")
					.append(ex.toString()).toString());
		}
	}

	protected double crossEntropy(double o1, double o2, double targetValue) {
		double oij = o1 - o2;
		double ce = -targetValue * oij + SimpleMath.logBase2(1.0D + Math.exp(oij));
		return ce;
	}

	protected void estimateLoss() {
		this.misorderedPairs = 0;
		this.error = 0.0D;
		int total = 0;
		for (int j = 0; j < this.samples.size(); j++) {
			RankList rl = ((RankList) this.samples.get(j)).getCorrectRanking();
			for (int k = 0; k < rl.size() - 1; k++) {
				double o1 = eval(rl.get(k));
				for (int l = k + 1; l < rl.size(); l++) {
					if (rl.get(k).getLabel() > rl.get(l).getLabel()) {
						total++;
						double o2 = eval(rl.get(l));
						this.error += crossEntropy(o1, o2, 1.0D);

						if (o1 <= o2) {
							this.misorderedPairs += 1;
						}

					}

				}

			}

		}

		this.error = SimpleMath.round(this.error / this.totalPairs, 4);

		this.lastError = this.error;
	}

	public void init() {
		PRINT("Initializing... ");

		setInputOutput(this.features.length, 1);
		for (int i = 0; i < nHiddenLayer; i++)
			addHiddenLayer(nHiddenNodePerLayer);
		wire();

		this.totalPairs = 0;
		for (int i = 0; i < this.samples.size(); i++) {
			RankList rl = ((RankList) this.samples.get(i)).getCorrectRanking();
			for (int j = 0; j < rl.size() - 1; j++)
				for (int k = j + 1; k < rl.size(); k++)
					if (rl.get(j).getLabel() > rl.get(k).getLabel())
						this.totalPairs += 1;
		}
		System.out.println(new StringBuilder().append("Query: ").append(this.samples.size()).toString());
		System.out.println(new StringBuilder().append("Total: ").append(this.totalPairs).toString());

		if (this.validationSamples != null) {
			for (int i = 0; i < this.layers.size(); i++)
				this.bestModelOnValidation.add(new ArrayList());
		}
		Neuron.learningRate = learningRate;
		PRINTLN("[Done]");

		estimateLoss();
		System.out.println("csc");
		System.out.println(
				new StringBuilder().append("res: ").append(SimpleMath.round(this.misorderedPairs / this.totalPairs, 4))
						.append("\t").append(this.misorderedPairs).append("\t").append(this.totalPairs).toString());
	}

	public void learn() {
		PRINTLN("-----------------------------------------");
		PRINTLN("Training starts...");
		PRINTLN("--------------------------------------------------");
		PRINTLN(new int[] { 7, 14, 9, 9 },
				new String[] { "#epoch", "% mis-ordered",
						new StringBuilder().append(this.scorer.name()).append("-T").toString(),
						new StringBuilder().append(this.scorer.name()).append("-V").toString() });
		PRINTLN(new int[] { 7, 14, 9, 9 }, new String[] { " ", "  pairs", " ", " " });
		PRINTLN("--------------------------------------------------");

		for (int i = 1; i <= nIteration; i++) {
			for (int j = 0; j < this.samples.size(); j++) {
				RankList rl = internalReorder((RankList) this.samples.get(j));
				int[][] pairMap = batchFeedForward(rl);
				float[][] pairWeight = computePairWeight(pairMap, rl);
				batchBackPropagate(pairMap, pairWeight);
				clearNeuronOutputs();
			}

			this.scoreOnTrainingData = this.scorer.score(rank(this.samples));
			estimateLoss();

			PRINT(new int[] { 7, 14 },
					new String[] { new StringBuilder().append(i).append("").toString(),
							new StringBuilder().append(SimpleMath.round(this.misorderedPairs / this.totalPairs, 4))
									.append("").toString() });

			if (i % 1 == 0) {
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
		for (int k = 0; k < this.inputLayer.size() - 1; k++) {
			this.inputLayer.get(k).setOutput(p.getFeatureValue(this.features[k]));
		}
		this.inputLayer.get(this.inputLayer.size() - 1).setOutput(1.0D);

		for (int k = 1; k < this.layers.size(); k++)
			((Layer) this.layers.get(k)).computeOutput();
		return this.outputLayer.get(0).getOutput();
	}

	public Ranker clone() {
		return new RankNet();
	}

	public String toString() {
		String output = "";
		for (int i = 0; i < this.layers.size() - 1; i++) {
			for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++) {
				output = new StringBuilder().append(output).append(i).append(" ").append(j).append(" ").toString();
				Neuron n = ((Layer) this.layers.get(i)).get(j);
				for (int k = 0; k < n.getOutLinks().size(); k++)
					output = new StringBuilder().append(output).append(((Synapse) n.getOutLinks().get(k)).getWeight())
							.append(k == n.getOutLinks().size() - 1 ? "" : " ").toString();
				output = new StringBuilder().append(output).append("\n").toString();
			}
		}
		return output;
	}

	public String model() {
		String output = new StringBuilder().append("## ").append(name()).append("\n").toString();
		output = new StringBuilder().append(output).append("## Epochs = ").append(nIteration).append("\n").toString();
		output = new StringBuilder().append(output).append("## No. of features = ").append(this.features.length)
				.append("\n").toString();
		output = new StringBuilder().append(output).append("## No. of hidden layers = ").append(this.layers.size() - 2)
				.append("\n").toString();
		for (int i = 1; i < this.layers.size() - 1; i++) {
			output = new StringBuilder().append(output).append("## Layer ").append(i).append(": ")
					.append(((Layer) this.layers.get(i)).size()).append(" neurons\n").toString();
		}

		for (int i = 0; i < this.features.length; i++)
			output = new StringBuilder().append(output).append(this.features[i])
					.append(i == this.features.length - 1 ? "" : " ").toString();
		output = new StringBuilder().append(output).append("\n").toString();

		output = new StringBuilder().append(output).append(this.layers.size() - 2).append("\n").toString();
		for (int i = 1; i < this.layers.size() - 1; i++) {
			output = new StringBuilder().append(output).append(((Layer) this.layers.get(i)).size()).append("\n")
					.toString();
		}
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
					.println(new StringBuilder().append("Error in RankNet::load(): ").append(ex.toString()).toString());
		}
	}

	public void printParameters() {
		PRINTLN(new StringBuilder().append("No. of epochs: ").append(nIteration).toString());
		PRINTLN(new StringBuilder().append("No. of hidden layers: ").append(nHiddenLayer).toString());
		PRINTLN(new StringBuilder().append("No. of hidden nodes per layer: ").append(nHiddenNodePerLayer).toString());
		PRINTLN(new StringBuilder().append("Learning rate: ").append(learningRate).toString());
	}

	public String name() {
		return "RankNet";
	}

	protected void printNetworkConfig() {
		for (int i = 1; i < this.layers.size(); i++) {
			System.out.println(new StringBuilder().append("Layer-").append(i + 1).toString());
			for (int j = 0; j < ((Layer) this.layers.get(i)).size(); j++) {
				Neuron n = ((Layer) this.layers.get(i)).get(j);
				System.out.print(new StringBuilder().append("Neuron-").append(j + 1).append(": ")
						.append(n.getInLinks().size()).append(" inputs\t").toString());
				for (int k = 0; k < n.getInLinks().size(); k++)
					System.out.print(new StringBuilder().append(((Synapse) n.getInLinks().get(k)).getWeight())
							.append("\t").toString());
				System.out.println("");
			}
		}
	}

	protected void printWeightVector() {
		for (int j = 0; j < this.outputLayer.get(0).getInLinks().size(); j++)
			System.out.print(
					new StringBuilder().append(((Synapse) this.outputLayer.get(0).getInLinks().get(j)).getWeight())
							.append(" ").toString());
		System.out.println("");
	}
}