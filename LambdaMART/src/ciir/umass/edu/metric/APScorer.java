package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

public class APScorer extends MetricScorer {
	public Hashtable<String, Integer> relDocCount = null;

	public APScorer() {
		this.k = 0;
	}

	public MetricScorer clone() {
		return new APScorer();
	}

	public void loadExternalRelevanceJudgment(String qrelFile) {
		this.relDocCount = new Hashtable();
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(qrelFile)));
			String lastQID = "";
			int rdCount = 0;
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if (content.length() == 0)
					continue;
				String[] s = content.split(" ");
				String qid = s[0].trim();

				int label = Integer.parseInt(s[3].trim());
				if ((lastQID.compareTo("") != 0) && (lastQID.compareTo(qid) != 0)) {
					this.relDocCount.put(lastQID, Integer.valueOf(rdCount));
					rdCount = 0;
				}
				lastQID = qid;
				if (label > 0)
					rdCount++;
			}
			this.relDocCount.put(lastQID, Integer.valueOf(rdCount));
			in.close();
			System.out.println("Relevance judgment file loaded. [#q=" + this.relDocCount.keySet().size() + "]");
		} catch (Exception ex) {
			System.out.println("Error in APScorer::loadExternalRelevanceJudgment(): " + ex.toString());
		}
	}

	public double score(RankList rl) {
		double ap = 0.0D;
		int count = 0;
		for (int i = 0; i < rl.size(); i++) {
			if (rl.get(i).getLabel() <= 0.0D)
				continue;
			count++;
			ap += count / (i + 1);
		}

		int rdCount = 0;
		if (this.relDocCount != null) {
			Integer it = (Integer) this.relDocCount.get(rl.getID());
			if (it != null)
				rdCount = it.intValue();
		} else {
			rdCount = count;
		}
		if (rdCount == 0)
			return 0.0D;
		return ap / rdCount;
	}

	public String name() {
		return "MAP";
	}

	public double[][] swapChange(RankList rl) {
		int[] relCount = new int[rl.size()];
		int[] labels = new int[rl.size()];
		int count = 0;
		for (int i = 0; i < rl.size(); i++) {
			if (rl.get(i).getLabel() > 0.0F) {
				labels[i] = 1;
				count++;
			} else {
				labels[i] = 0;
			}
			relCount[i] = count;
		}
		int rdCount = 0;
		if (this.relDocCount != null) {
			Integer it = (Integer) this.relDocCount.get(rl.getID());
			if (it != null)
				rdCount = it.intValue();
		} else {
			rdCount = count;
		}
		double[][] changes = new double[rl.size()][];
		for (int i = 0; i < rl.size(); i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0.0D);
		}

		if ((rdCount == 0) || (count == 0)) {
			return changes;
		}
		for (int i = 0; i < rl.size() - 1; i++) {
			for (int j = i + 1; j < rl.size(); j++) {
				double change = 0.0D;
				if (labels[i] != labels[j]) {
					int diff = labels[j] - labels[i];
					change += ((relCount[i] + diff) * labels[j] - relCount[i] * labels[i]) / (i + 1);
					for (int k = i + 1; k <= j - 1; k++)
						if (labels[k] > 0)
							change += (relCount[k] + diff) / (k + 1);
					change += -relCount[j] * diff / (j + 1);
				}
				double tmp351_350 = (change / rdCount);
				changes[i][j] = tmp351_350;
				changes[j][i] = tmp351_350;
			}
		}
		return changes;
	}
}