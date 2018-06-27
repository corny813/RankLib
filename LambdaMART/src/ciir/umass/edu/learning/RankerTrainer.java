package ciir.umass.edu.learning;

import ciir.umass.edu.metric.MetricScorer;
import java.util.List;

public class RankerTrainer {
	protected RankerFactory rf = new RankerFactory();

	public Ranker train(RANKER_TYPE type, List<RankList> samples, int[] features, MetricScorer scorer) {
		Ranker ranker = this.rf.createRanker(type, samples, features);
		ranker.set(scorer);
		ranker.init();
		ranker.learn();
		return ranker;
	}
}