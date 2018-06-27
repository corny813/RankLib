package ciir.umass.edu.features;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.util.Arrays;

public class ZScoreNormalizor implements Normalizer {
	public void normalize(RankList rl, int[] fids) {
		float[] mean = new float[fids.length];
		float[] std = new float[fids.length];
		Arrays.fill(mean, 0.0F);
		Arrays.fill(std, 0.0F);
		for (int i = 0; i < rl.size(); i++) {
			DataPoint dp = rl.get(i);
			for (int j = 0; j < fids.length; j++) {
				mean[j] += dp.getFeatureValue(fids[j]);
			}
		}
		for (int j = 0; j < fids.length; j++) {
			mean[j] /= rl.size();
			for (int i = 0; i < rl.size(); i++) {
				DataPoint p = rl.get(i);
				float x = p.getFeatureValue(fids[j]) - mean[j];
				std[j] += x * x;
			}
			std[j] = (float) Math.sqrt(std[j] / (rl.size() - 1));

			if (std[j] <= 0.0D)
				continue;
			for (int i = 0; i < rl.size(); i++) {
				DataPoint p = rl.get(i);
				float x = (p.getFeatureValue(fids[j]) - mean[j]) / std[j];
				p.setFeatureValue(fids[j], x);
			}
		}
	}

	public String name() {
		return "zscore";
	}
}