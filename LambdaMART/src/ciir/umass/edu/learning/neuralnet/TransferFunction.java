package ciir.umass.edu.learning.neuralnet;

public abstract interface TransferFunction {
	public abstract double compute(double paramDouble);

	public abstract double computeDerivative(double paramDouble);
}