package ciir.umass.edu.learning.neuralnet;

import java.util.List;

public class ListNeuron extends Neuron {
	protected double[] d1;
	protected double[] d2;

	public void computeDelta(PropParameter param) {
		double sumLabelExp = 0.0D;
		double sumScoreExp = 0.0D;
		for (int i = 0; i < this.outputs.size(); i++) {
			sumLabelExp += Math.exp(param.labels[i]);
			sumScoreExp += Math.exp(((Double) this.outputs.get(i)).doubleValue());
		}

		this.d1 = new double[this.outputs.size()];
		this.d2 = new double[this.outputs.size()];
		for (int i = 0; i < this.outputs.size(); i++) {
			this.d1[i] = (Math.exp(param.labels[i]) / sumLabelExp);
			this.d2[i] = (Math.exp(((Double) this.outputs.get(i)).doubleValue()) / sumScoreExp);
		}
	}

	public void updateWeight(PropParameter param) {
		Synapse s = null;
		for (int k = 0; k < this.inLinks.size(); k++) {
			s = (Synapse) this.inLinks.get(k);
			double dw = 0.0D;
			for (int l = 0; l < this.d1.length; l++) {
				dw += (this.d1[l] - this.d2[l]) * s.getSource().getOutput(l);
			}
			dw *= learningRate;
			s.setWeightAdjustment(dw);
			s.updateWeight();
		}
	}
}