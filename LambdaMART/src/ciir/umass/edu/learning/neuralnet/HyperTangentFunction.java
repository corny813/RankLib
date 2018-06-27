package ciir.umass.edu.learning.neuralnet;

public class HyperTangentFunction implements TransferFunction {
	public double compute(double x) {
		return 1.7159D * Math.tanh(x * 2.0D / 3.0D);
	}

	public double computeDerivative(double x) {
		double output = Math.tanh(x * 2.0D / 3.0D);
		return 1.7159D * (1.0D - output * output) * 2.0D / 3.0D;
	}
}