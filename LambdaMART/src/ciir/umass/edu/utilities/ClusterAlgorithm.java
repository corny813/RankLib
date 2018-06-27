package ciir.umass.edu.utilities;

import java.io.PrintStream;
import java.util.Arrays;

public class ClusterAlgorithm {
	public int[] Kmeans(float[] data, int k, int maxIteration) {
		float minValue = 3.4028235E+38F;
		float maxValue = 1.4E-45F;
		for (int i = 0; i < data.length; i++) {
			if (minValue > data[i]) {
				minValue = data[i];
			}
			if (maxValue < data[i]) {
				maxValue = data[i];
			}
		}

		float[] kenels = new float[k];
		int[] dataType = new int[data.length];
		int[] selectKenels = new int[k];
		for (int i = 0; i < k; i++) {
			kenels[i] = ((maxValue - minValue) / (k + 1) * (i + 1) + minValue);
		}

		for (int iter = 0; iter < maxIteration; iter++) {
			sortArray(kenels);
			float error = 0.0F;
			for (int i = 0; i < data.length; i++) {
				int index = findMinDisKenel(kenels, data[i]);
				dataType[i] = index;
			}
			error = updateKenels(kenels, data, dataType);
			if (error < 0.01D) {
				break;
			}

		}

		return dataType;
	}

	private float updateKenels(float[] kenels, float[] data, int[] dataType) {
		float error = 0.0F;
		int[] kenelsDataNum = new int[kenels.length];
		float[] oldkenels = new float[kenels.length];
		for (int i = 0; i < oldkenels.length; i++) {
			oldkenels[i] = kenels[i];
		}
		Arrays.fill(kenelsDataNum, 0);
		Arrays.fill(kenels, 0.0F);
		for (int i = 0; i < data.length; i++) {
			kenels[dataType[i]] += data[i];
			kenelsDataNum[dataType[i]] += 1;
		}
		for (int i = 0; i < kenels.length; i++) {
			if (kenelsDataNum[i] < 1) {
				kenels[i] = oldkenels[i];
			} else {
				kenels[i] /= kenelsDataNum[i];
				error += Math.abs(kenels[i] - oldkenels[i]);
			}
		}
		return error;
	}

	private void sortArray(float[] kenels) {
		for (int i = 0; i < kenels.length; i++)
			for (int j = 0; j < kenels.length - 1; j++)
				if (kenels[j] > kenels[(j + 1)]) {
					float tmp = kenels[(j + 1)];
					kenels[(j + 1)] = kenels[j];
					kenels[j] = tmp;
				}
	}

	private int[] sortArrayIndex(int[] kenels) {
		int[] sortIndex = new int[kenels.length];
		for (int i = 0; i < kenels.length; i++) {
			sortIndex[i] = i;
		}

		for (int i = 0; i < kenels.length; i++) {
			for (int j = 0; j < kenels.length - 1; j++) {
				if (kenels[j] < kenels[(j + 1)]) {
					int tmp = kenels[(j + 1)];
					kenels[(j + 1)] = kenels[j];
					kenels[j] = tmp;
					tmp = sortIndex[(j + 1)];
					sortIndex[(j + 1)] = sortIndex[j];
					sortIndex[j] = tmp;
				}
			}
		}

		return sortIndex;
	}

	private int findMinDisKenel(float[] kenels, float value) {
		int index = 0;

		if (value < kenels[0]) {
			return 0;
		}
		if (value > kenels[(kenels.length - 1)]) {
			return kenels.length - 1;
		}

		for (int i = 0; i < kenels.length - 1; i++) {
			if (value > kenels[(i + 1)]) {
				continue;
			}
			if (value - kenels[i] <= kenels[(i + 1)] - value) {
				return i;
			}
			return i + 1;
		}

		return index;
	}

	public float[] gaussianSingleModel(float[] data, int[] dataType, int k, float minRate) {
		int[] kenelDataNum = new int[k];
		for (int i = 0; i < dataType.length; i++) {
			kenelDataNum[dataType[i]] += 1;
		}
		int[] sortIndex = sortArrayIndex(kenelDataNum);

		int[] selectType = new int[k];
		Arrays.fill(selectType, 0);
		int selectNum = 0;
		for (int i = 0; (i < kenelDataNum.length) && (selectNum <= data.length * minRate); i++) {
			selectNum += kenelDataNum[i];
			selectType[sortIndex[i]] = 1;
		}

		float ave = 0.0F;
		float var = 0.0F;
		float kmeansNum = 0.0F;
		for (int i = 0; i < data.length; i++) {
			if (selectType[dataType[i]] == 0) {
				continue;
			}
			kmeansNum += 1.0F;
			ave += data[i];
			var += data[i] * data[i];
		}
		System.out.println("gaussian data num " + kmeansNum + " " + data.length);
		var -= ave / selectNum * ave;
		var /= selectNum;
		ave /= selectNum;

		if (var < 0.0F)
			var = 0.0F;
		var = (float) Math.sqrt(var);
		float[] res = new float[2];
		res[0] = ave;
		res[1] = var;

		return res;
	}

	public float[] getLeafGaussianParm(float[] data, int k, int maxIteration, float minRate) {
		int[] dataType = Kmeans(data, k, maxIteration);
		float[] gaussianParm = gaussianSingleModel(data, dataType, k, minRate);
		return gaussianParm;
	}
}