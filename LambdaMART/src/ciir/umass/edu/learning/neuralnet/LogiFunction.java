package ciir.umass.edu.learning.neuralnet;

public class LogiFunction implements TransferFunction {
	public double compute(double x) {
		return 1.0D / (1.0D + Math.exp(-x));
	}

	public double computeDerivative(double x) {
		double output = compute(x);
		return output * (1.0D - output);
	}
}