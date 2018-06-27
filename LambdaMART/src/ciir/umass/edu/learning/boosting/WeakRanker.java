package ciir.umass.edu.learning.boosting;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.Sorter;
import java.util.ArrayList;
import java.util.List;

public class WeakRanker {
	private int fid = -1;

	public WeakRanker(int fid) {
		this.fid = fid;
	}

	public int getFID() {
		return this.fid;
	}

	public RankList rank(RankList l) {
		double[] score = new double[l.size()];
		for (int i = 0; i < l.size(); i++)
			score[i] = l.get(i).getFeatureValue(this.fid);
		int[] idx = Sorter.sort(score, false);
		return new RankList(l, idx);
	}

	public List<RankList> rank(List<RankList> l) {
		List ll = new ArrayList();
		for (int i = 0; i < l.size(); i++)
			ll.add(rank((RankList) l.get(i)));
		return ll;
	}
}