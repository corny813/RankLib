package ciir.umass.edu.learning.neuralnet;

import java.util.ArrayList;
import java.util.List;

public class Neuron {
	public static double momentum = 0.9D;
	public static double learningRate = 0.001D;

	protected TransferFunction tfunc = new LogiFunction();
	protected double output;
	protected List<Double> outputs = null;
	protected double delta_i = 0.0D;
	protected double[] deltas_j = null;

	protected List<Synapse> inLinks = null;
	protected List<Synapse> outLinks = null;

	public Neuron() {
		this.output = 0.0D;
		this.inLinks = new ArrayList();
		this.outLinks = new ArrayList();

		this.outputs = new ArrayList();
		this.delta_i = 0.0D;
	}

	public double getOutput() {
		return this.output;
	}

	public double getOutput(int k) {
		return ((Double) this.outputs.get(k)).doubleValue();
	}

	public List<Synapse> getInLinks() {
		return this.inLinks;
	}

	public List<Synapse> getOutLinks() {
		return this.outLinks;
	}

	public void setOutput(double output) {
		this.output = output;
	}

	public void addOutput(double output) {
		this.outputs.add(Double.valueOf(output));
	}

	public void computeOutput() {
		Synapse s = null;
		double wsum = 0.0D;
		for (int j = 0; j < this.inLinks.size(); j++) {
			s = (Synapse) this.inLinks.get(j);
			wsum += s.getSource().getOutput() * s.getWeight();
		}
		this.output = this.tfunc.compute(wsum);
	}

	public void computeOutput(int i) {
		Synapse s = null;
		double wsum = 0.0D;
		for (int j = 0; j < this.inLinks.size(); j++) {
			s = (Synapse) this.inLinks.get(j);
			wsum += s.getSource().getOutput(i) * s.getWeight();
		}
		this.output = this.tfunc.compute(wsum);
		this.outputs.add(Double.valueOf(this.output));
	}

	public void clearOutputs() {
		this.outputs.clear();
	}

	public void computeDelta(PropParameter param) {
		int[][] pairMap = param.pairMap;
		int current = param.current;

		this.delta_i = 0.0D;
		this.deltas_j = new double[pairMap[current].length];
		for (int k = 0; k < pairMap[current].length; k++) {
			int j = pairMap[current][k];
			float weight = 1.0F;
			double pij = 0.0D;
			if (param.pairWeight == null) {
				weight = 1.0F;
				pij = 1.0D / (1.0D + Math.exp(((Double) this.outputs.get(current)).doubleValue()
						- ((Double) this.outputs.get(j)).doubleValue()));
			} else {
				weight = param.pairWeight[current][k];
				pij = param.targetValue[current][k]
						- 1.0D / (1.0D + Math.exp(-(((Double) this.outputs.get(current)).doubleValue()
								- ((Double) this.outputs.get(j)).doubleValue())));
			}
			double lambda = weight * pij;
			this.delta_i += lambda;
			this.deltas_j[k] = (lambda * this.tfunc.computeDerivative(((Double) this.outputs.get(j)).doubleValue()));
		}
		this.delta_i *= this.tfunc.computeDerivative(((Double) this.outputs.get(current)).doubleValue());
	}

	public void updateDelta(PropParameter param) {
		int[][] pairMap = param.pairMap;
		float[][] pairWeight = param.pairWeight;
		int current = param.current;
		this.delta_i = 0.0D;
		this.deltas_j = new double[pairMap[current].length];
		for (int k = 0; k < pairMap[current].length; k++) {
			int j = pairMap[current][k];
			float weight = pairWeight != null ? pairWeight[current][k] : 1.0F;
			double errorSum = 0.0D;
			for (int l = 0; l < this.outLinks.size(); l++) {
				Synapse s = (Synapse) this.outLinks.get(l);
				errorSum += s.getTarget().deltas_j[k] * s.weight;
				if (k == 0)
					this.delta_i += s.getTarget().delta_i * s.weight;
			}
			if (k == 0)
				this.delta_i *= weight
						* this.tfunc.computeDerivative(((Double) this.outputs.get(current)).doubleValue());
			this.deltas_j[k] = (errorSum * weight
					* this.tfunc.computeDerivative(((Double) this.outputs.get(j)).doubleValue()));
		}
	}

	public void updateWeight(PropParameter param) {
		Synapse s = null;
		for (int k = 0; k < this.inLinks.size(); k++) {
			s = (Synapse) this.inLinks.get(k);
			double sum_j = 0.0D;
			for (int l = 0; l < this.deltas_j.length; l++)
				sum_j += this.deltas_j[l] * s.getSource().getOutput(param.pairMap[param.current][l]);
			double dw = learningRate * (this.delta_i * s.getSource().getOutput(param.current) - sum_j);
			s.setWeightAdjustment(dw);
			s.updateWeight();
		}
	}
}