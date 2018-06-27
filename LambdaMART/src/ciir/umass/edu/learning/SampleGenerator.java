package ciir.umass.edu.learning;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SampleGenerator {
	private int nFeature = 30;

	int[] permuIndex1 = null;
	int[] permuIndex2 = null;
	int[] permuIndex3 = null;
	float[] constants = null;

	private Random rnd = new Random();

	public static void main(String[] args) {
		SampleGenerator sg = new SampleGenerator(50);

		int[] count = sg.generate(2000, "data/toy/train.txt");

		System.out.print("#{");
		for (int i = 0; i < count.length; i++)
			System.out.print(new StringBuilder().append(count[i]).append(i == count.length - 1 ? "" : ", ").toString());
		System.out.println("}");

		System.out.println("#Validation");
		sg.generate(0.2F, count, "data/toy/vali.txt");

		System.out.println("#Test");
		sg.generate(1.0F, count, "data/toy/test.txt");
	}

	public SampleGenerator(int nFeature) {
		this.nFeature = nFeature;

		this.permuIndex1 = getPermutationIndex();
		this.permuIndex2 = getPermutationIndex();
		this.permuIndex3 = getPermutationIndex();
		this.constants = randomVector();
	}

	private int[] getPermutationIndex() {
		int[] pi = new int[this.nFeature];
		List l = new ArrayList();
		for (int i = 0; i < this.nFeature; i++)
			l.add(Integer.valueOf(i));
		Collections.shuffle(l);
		for (int i = 0; i < this.nFeature; i++)
			pi[i] = ((Integer) l.get(i)).intValue();
		return pi;
	}

	public int[] generate(int nSamples, String outputFile) {
		int[] count = null;
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "ASCII"));

			int[][] dists = { { 10, 2, 1, 2 }, { 12, 1, 1, 1 }, { 13, 0, 0, 2 }, { 12, 0, 1, 2 }, { 13, 2, 0, 0 } };

			float[] prob = { 0.0F, 0.2F, 0.4F, 0.6F, 0.8F };

			count = new int[dists.length];
			for (int i = 0; i < dists.length; i++) {
				count[i] = 0;
			}
			for (int i = 0; i < nSamples; i++) {
				float v = this.rnd.nextFloat();
				int[] dist = null;
				for (int j = prob.length - 1; j >= 0; j--) {
					if (v < prob[j])
						continue;
					dist = dists[j];
					count[j] += 1;
					break;
				}

				generate(new StringBuilder().append(i + 1).append("").toString(), dist, out);
			}
			out.close();
		} catch (Exception localException) {
		}

		return count;
	}

	public void generate(float ratio, int[] count, String outputFile) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "ASCII"));
			int[][] dists = { { 10, 2, 1, 2 }, { 12, 1, 1, 1 }, { 13, 0, 0, 2 }, { 12, 0, 1, 2 }, { 13, 2, 0, 0 } };

			int[] taken = new int[count.length];
			for (int i = 0; i < taken.length; i++) {
				taken[i] = 0;
				count[i] = (int) (count[i] * ratio);
			}

			int k = 0;
			while (true) {
				int j = this.rnd.nextInt(5);
				if (taken[j] <= count[j]) {
					generate(new StringBuilder().append(k + 1).append("").toString(), dists[j], out);
					taken[j] += 1;
					k++;
				}

				boolean flag = true;
				for (int i = 0; (i < taken.length) && (flag); i++)
					if (taken[i] < count[i])
						flag = false;
				if (flag)
					break;
			}
			out.close();
		} catch (Exception localException) {
		}
	}

	public void generate(String id, int[] dist, BufferedWriter out) throws Exception {
		float[] t = { -1000000.0F, -1.0F, 1.0F, 2.0F };
		int[] taken = { 0, 0, 0, 0 };
		while (true) {
			float[] f = randomVector();
			double v = eval(f);
			int label = 0;
			for (int j = t.length - 1; j >= 0; j--) {
				if (v <= t[j])
					continue;
				label = j;
				taken[j] += 1;
				if (taken[j] > dist[j])
					break;
				write(id, f, label, out);
				break;
			}

			boolean flag = true;
			for (int i = 0; (i < taken.length) && (flag); i++)
				if (taken[i] < dist[i])
					flag = false;
			if (flag)
				break;
		}
	}

	public float[] randomVector() {
		float[] f = new float[this.nFeature];
		for (int i = 0; i < this.nFeature; i++)
			f[i] = (this.rnd.nextFloat() * (this.rnd.nextInt(2) == 0 ? 1 : -1));
		return f;
	}

	public double eval(float[] features) {
		double val = 0.0D;

		for (int i = 0; i < this.nFeature; i++) {
			double v0 = features[i] * this.constants[i];
			double v1 = features[i] * features[this.permuIndex1[i]];
			double v2 = features[i] * features[this.permuIndex2[i]] * features[this.permuIndex3[i]];
			val += v0 + v1 + v2;
		}
		val /= 3.0D;
		return val;
	}

	public void write(String id, float[] features, int label, BufferedWriter out) throws Exception {
		out.write(new StringBuilder().append(label).append(" qid:").append(id).append(" ").toString());
		for (int i = 0; i < features.length; i++)
			out.write(new StringBuilder().append(i + 1).append(":").append(features[i])
					.append(i == features.length - 1 ? "" : " ").toString());
		out.newLine();
	}
}