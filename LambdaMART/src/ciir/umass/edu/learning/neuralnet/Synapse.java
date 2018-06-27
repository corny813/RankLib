package ciir.umass.edu.learning.neuralnet;

import java.util.List;
import java.util.Random;

public class Synapse {
	static Random random = new Random();
	protected double weight = 0.0D;
	protected double dW = 0.0D;
	protected Neuron source = null;
	protected Neuron target = null;

	public Synapse(Neuron source, Neuron target) {
		this.source = source;
		this.target = target;
		this.source.getOutLinks().add(this);
		this.target.getInLinks().add(this);

		this.weight = ((random.nextInt(2) == 0 ? 1 : -1) * random.nextFloat() / 10.0F);
	}

	public Neuron getSource() {
		return this.source;
	}

	public Neuron getTarget() {
		return this.target;
	}

	public void setWeight(double w) {
		this.weight = w;
	}

	public double getWeight() {
		return this.weight;
	}

	public double getLastWeightAdjustment() {
		return this.dW;
	}

	public void setWeightAdjustment(double dW) {
		this.dW = dW;
	}

	public void updateWeight() {
		this.weight += this.dW;
	}
}