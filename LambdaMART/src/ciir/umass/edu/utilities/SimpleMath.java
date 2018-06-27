package ciir.umass.edu.utilities;

public class SimpleMath {
	public static double logBase2(double value) {
		return Math.log(value) / Math.log(2.0D);
	}

	public static double logBase10(double value) {
		return Math.log(value) / Math.log(10.0D);
	}

	public static double ln(double value) {
		return Math.log(value) / Math.log(2.718281828459045D);
	}

	public static int min(int a, int b) {
		return a > b ? b : a;
	}

	public static double p(long count, long total) {
		return (count + 0.5D) / (total + 1L);
	}

	public static double round(double val) {
		int precision = 10000;
		return Math.floor(val * precision + 0.5D) / precision;
	}

	public static double round(float val) {
		int precision = 10000;
		return Math.floor(val * precision + 0.5D) / precision;
	}

	public static double round(double val, int n) {
		int precision = 1;
		for (int i = 0; i < n; i++)
			precision *= 10;
		return Math.floor(val * precision + 0.5D) / precision;
	}

	public static float round(float val, int n) {
		int precision = 1;
		for (int i = 0; i < n; i++)
			precision *= 10;
		return (float) (Math.floor(val * precision + 0.5D) / precision);
	}
}