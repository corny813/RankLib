package ciir.umass.edu.metric;

import java.util.Hashtable;

import ciir.umass.edu.learning.RankList;

public class MetricScorerFactory {
	private static MetricScorer[] mFactory = { new APScorer(), new NDCGScorer(), new DCGScorer(), new PrecisionScorer(),
			new ReciprocalRankScorer(), new BestAtKScorer(), new ERRScorer() };
	private static Hashtable<String, MetricScorer> map = new Hashtable();

	public MetricScorerFactory() {
		map.put("MAP", new APScorer());
		map.put("NDCG", new NDCGScorer());
		map.put("DCG", new DCGScorer());
		map.put("P", new PrecisionScorer());
		map.put("RR", new ReciprocalRankScorer());
		map.put("BEST", new BestAtKScorer());
		map.put("ERR", new ERRScorer());
	}

	public int[] getRelevanceLabels(RankList rl) {
		int[] rel = new int[rl.size()];
		for (int i = 0; i < rl.size(); i++)
			rel[i] = (int) rl.get(i).getLabel();
		return rel;
	}

	public MetricScorer createScorer(METRIC metric) {
		return mFactory[(metric.ordinal() - METRIC.MAP.ordinal())].clone();
	}

	public MetricScorer createScorer(METRIC metric, int k) {
		MetricScorer s = mFactory[(metric.ordinal() - METRIC.MAP.ordinal())].clone();
		s.setK(k);
		return s;
	}

	public MetricScorer createScorer(String metric) {
		int k = -1;
		String m = "";
		MetricScorer s = null;
		if (metric.indexOf("@") != -1) {
			m = metric.substring(0, metric.indexOf("@"));
			k = Integer.parseInt(metric.substring(metric.indexOf("@") + 1));
			s = ((MetricScorer) map.get(m.toUpperCase())).clone();
			s.setK(k);
		} else {
			s = ((MetricScorer) map.get(metric.toUpperCase())).clone();
		}
		return s;
	}
}