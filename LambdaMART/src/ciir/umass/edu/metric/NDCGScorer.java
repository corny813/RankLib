package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.SimpleMath;
import ciir.umass.edu.utilities.Sorter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class NDCGScorer extends DCGScorer {
	protected HashMap<String, Double> idealGains = null;

	public NDCGScorer() {
		super();
		this.k = 10;
		idealGains = new HashMap<>();
	}

	public NDCGScorer(int k) {
		super();
		this.k = k;
		idealGains = new HashMap<>();
	}

	public MetricScorer clone() {
		return new NDCGScorer();
	}

	public void loadExternalRelevanceJudgment(String qrelFile) {
		this.idealGains = new HashMap<>();
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(qrelFile)));
			String lastQID = "";
			List rel = new ArrayList();
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if (content.length() == 0)
					continue;
				String[] s = content.split(" ");
				String qid = s[0].trim();

				int label = Integer.parseInt(s[3].trim());
				if ((lastQID.compareTo("") != 0) && (lastQID.compareTo(qid) != 0)) {
					double ideal = getIdealDCG(rel, this.k);
					this.idealGains.put(lastQID, Double.valueOf(ideal));
					rel.clear();
				}
				lastQID = qid;
				rel.add(Integer.valueOf(label));
			}
			if (rel.size() > 0) {
				double ideal = getIdealDCG(rel, this.k);
				this.idealGains.put(lastQID, Double.valueOf(ideal));
				rel.clear();
			}
			in.close();
			System.out.println("Relevance judgment file loaded. [#q=" + this.idealGains.keySet().size() + "]");
		} catch (Exception ex) {
			System.out.println("Error in NDCGScorer::loadExternalRelevanceJudgment(): " + ex.toString());
		}
	}

	public double score(RankList rl) {
		if(rl.size() == 0)
			return 0;

		int size = k;
		if(k > rl.size() || k <= 0)
			size = rl.size();
		
		int[] rel = getRelevanceLabels(rl);
		
		double ideal = 0;
		Double d = idealGains.get(rl.getID());
		if ( d != null ) {
			ideal = d;
		} else {
			ideal = getIdealDCG(rel, size);
			idealGains.put(rl.getID(), ideal);
		}
		
		if(ideal <= 0.0)//I mean precisely "="
			return 0.0;

		return getDCG(rel, size)/ideal;
	}

	public String name() {
		return "NDCG@" + this.k;
	}

	private double getIdealDCG(List<Integer> rel, int k) {
		int size = k;
		if ((k > rel.size()) || (k <= 0)) {
			size = rel.size();
		}
		int[] idx = Sorter.sort(rel, false);
		double dcg = 0.0D;
		for (int i = 1; i <= size; i++) {
			dcg += (Math.pow(2.0D, ((Integer) rel.get(idx[(i - 1)])).intValue()) - 1.0D) / SimpleMath.logBase2(i + 1);
		}
		return dcg;
	}

	private double getIdealDCG(int[] rel, int topK) {
		int[] idx = Sorter.sort(rel, false);
		double dcg = 0;
		for (int i = 0; i < topK; i++)
			dcg += gain(rel[idx[i]]) * discount(i);
		return dcg;
	}

	public double[][] swapChange(RankList rl) {
		int size = (rl.size() > k) ? k : rl.size();
		// compute the ideal ndcg
		int[] rel = getRelevanceLabels(rl);
		double ideal = 0;
		Double d = idealGains.get(rl.getID());
		if (d != null)
			ideal = d;
		else {
			ideal = getIdealDCG(rel, size);
			// idealGains.put(rl.getID(), ideal);//DO *NOT* do caching here.
			// It's not thread-safe.
		}

		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0);
		}

		for (int i = 0; i < size; i++)
			for (int j = i + 1; j < rl.size(); j++)
				if (ideal > 0)
					changes[j][i] = changes[i][j] = (discount(i) - discount(j)) * (gain(rel[i]) - gain(rel[j])) / ideal;

		return changes;
	}

	public double[][] swapChange2(RankList rl) {
		int size = rl.size() > this.k ? this.k : rl.size();

		List rel = new ArrayList();
		for (int t = 0; t < rl.size(); t++) {
			rel.add(Integer.valueOf((int) rl.get(t).getLabel()));
		}
		double d2 = 0.0D;
		if (this.idealGains != null) {
			Double d = (Double) this.idealGains.get(rl.getID());
			if (d != null)
				d2 = d.doubleValue();
		} else {
			d2 = getIdealDCG(rel, size);
		}

		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0.0D);
		}

		for (int i = 0; i < size; i++) {
			int p1 = i + 1;
			for (int j = i + 1; j < rl.size(); j++) {
				if (d2 <= 0.0D)
					continue;
				int p2 = j + 1;
				double tmp290_289 = ((1.0D / SimpleMath.logBase2(p1 + 1) - 1.0D / SimpleMath.logBase2(p2 + 1))
						* (Math.pow(2.0D, ((Integer) rel.get(i)).intValue())
								- Math.pow(2.0D, ((Integer) rel.get(j)).intValue()))
						/ d2);
				changes[i][j] = tmp290_289;
				changes[j][i] = tmp290_289;
			}
		}

		return changes;
	}
}