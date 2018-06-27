package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import java.util.List;

public class MART extends LambdaMART {
	public MART() {
	}

	public MART(List<RankList> samples, int[] features) {
		super(samples, features);
	}

	public Ranker clone() {
		return new MART();
	}

	public String name() {
		return "MART";
	}

	protected void computePseudoResponses() {
		for (int i = 0; i < this.martSamples.length; i++)
			this.pseudoResponses[i] = (this.martSamples[i].getLabel() - this.modelScores[i]);
	}

	protected void updateTreeOutput(RegressionTree rt) {
		List leaves = rt.leaves();
		for (int i = 0; i < leaves.size(); i++) {
			float s1 = 0.0F;
			Split s = (Split) leaves.get(i);
			int[] idx = s.getSamples();
			for (int j = 0; j < idx.length; j++) {
				int k = idx[j];
				s1 += this.pseudoResponses[k];
			}
			s.setOutput(s1 / idx.length);
		}
	}
}