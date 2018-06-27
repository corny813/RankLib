package ciir.umass.edu.learning.neuralnet;

import java.util.ArrayList;
import java.util.List;

public class Layer {
	protected List<Neuron> neurons = null;

	public Layer(int size) {
		this.neurons = new ArrayList();
		for (int i = 0; i < size; i++)
			this.neurons.add(new Neuron());
	}

	public Layer(int size, int nType) {
		this.neurons = new ArrayList();
		for (int i = 0; i < size; i++)
			if (nType == 0)
				this.neurons.add(new Neuron());
			else
				this.neurons.add(new ListNeuron());
	}

	public Neuron get(int k) {
		return (Neuron) this.neurons.get(k);
	}

	public int size() {
		return this.neurons.size();
	}

	public void computeOutput(int i) {
		for (int j = 0; j < this.neurons.size(); j++)
			((Neuron) this.neurons.get(j)).computeOutput(i);
	}

	public void computeOutput() {
		for (int j = 0; j < this.neurons.size(); j++)
			((Neuron) this.neurons.get(j)).computeOutput();
	}

	public void clearOutputs() {
		for (int i = 0; i < this.neurons.size(); i++)
			((Neuron) this.neurons.get(i)).clearOutputs();
	}

	public void computeDelta(PropParameter param) {
		for (int i = 0; i < this.neurons.size(); i++)
			((Neuron) this.neurons.get(i)).computeDelta(param);
	}

	public void updateDelta(PropParameter param) {
		for (int i = 0; i < this.neurons.size(); i++)
			((Neuron) this.neurons.get(i)).updateDelta(param);
	}

	public void updateWeight(PropParameter param) {
		for (int i = 0; i < this.neurons.size(); i++)
			((Neuron) this.neurons.get(i)).updateWeight(param);
	}
}