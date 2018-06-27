package ciir.umass.edu.features;

import ciir.umass.edu.learning.RankList;

public abstract interface Normalizer {
	public abstract void normalize(RankList paramRankList, int[] paramArrayOfInt);

	public abstract String name();
}