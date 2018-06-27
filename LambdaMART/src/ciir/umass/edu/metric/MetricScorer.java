package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class MetricScorer {
	protected int k = 10;

	public void setK(int k) {
		this.k = k;
	}

	public void loadExternalRelevanceJudgment(String qrelFile) {
	}

	public double score(List<RankList> rl) {
		double score = 0.0D;
		for (int i = 0; i < rl.size(); i++)
			score += score((RankList) rl.get(i));
		return score / rl.size();
	}
	
	protected int[] getRelevanceLabels(RankList rl) {
		int[] rel = new int[rl.size()];
		for(int i=0;i<rl.size();i++)
			rel[i] = (int)rl.get(i).getLabel();
		return rel;
	}

	public double maxScore(List<RankList> rl) {
		double score = 0.0D;
		for (int i = 0; i < rl.size(); i++) {
			RankList origin = (RankList) rl.get(i);
			RankList[] maxSort = new RankList[6];
			for (int j = 0; j < 6; j++) {
				maxSort[j] = new RankList();
			}
			RankList maxSort_ = new RankList();
			for (int j = 0; j < origin.size(); j++) {
				if ((int) origin.get(j).getLabel() >= 0)
					maxSort[(int) origin.get(j).getLabel()].add(origin.get(j));
			}
			for (int j = 5; j > -1; j--) {
				for (int index = 0; index < maxSort[j].size(); index++) {
					maxSort_.add(maxSort[j].get(index));
				}
			}
			score += score(maxSort_);
		}
//		System.out.println("---maxScore = "+(score/rl.size())+" score="+score+" rl.size="+rl.size());
		return score / rl.size();
	}

	public double scoreERR3(List<RankList> rl, String outputFile) {
		double score = 0.0D;
		int totalNum = 0;
		BufferedWriter out = null;
		BufferedWriter outQuery = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true)));
			outQuery = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile + "__query", true)));
			out.write("ERR3\n");
			outQuery.write("ERR3\n");
			for (int i = 0; i < rl.size(); i++) {
				double cs = scoreERR3((RankList) rl.get(i));
				if (cs >= 0.0D) {
					totalNum++;
					score += cs;
					out.write(((RankList) rl.get(i)).getID() + " " + cs + "\n");
					outQuery.write(((RankList) rl.get(i)).get(0).getDescription() + " " + cs + "\n");
				}
			}
			outQuery.close();
			out.close();
		} catch (Exception e) {
			return -1.0D;
		}
		return score / totalNum;
	}

	public double scoreWithNotLabel(List<RankList> rl, String outputFile) {
		double score = 0.0D;
		BufferedWriter out = null;
		BufferedWriter outQuery = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true)));
			outQuery = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile + "__query", true)));
			out.write("WithNotLabel\n");
			outQuery.write("WithNotLabel\n");
			for (int i = 0; i < rl.size(); i++) {
				double cs = scoreWithNotLabel((RankList) rl.get(i));
				score += cs;
				out.write(((RankList) rl.get(i)).getID() + " " + cs + "\n");
				outQuery.write(((RankList) rl.get(i)).get(0).getDescription() + " " + cs + "\n");
			}
			outQuery.close();
			out.close();
		} catch (Exception e) {
			return -1.0D;
		}
		return score / rl.size();
	}

	public double scoreWithoutNotLabel(List<RankList> rl, String outputFile) {
		double score = 0.0D;
		BufferedWriter out = null;
		BufferedWriter outQuery = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true)));
			outQuery = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile + "__query", true)));
			out.write("WithoutNotLabel\n");
			outQuery.write("WithoutNotLabel\n");
			for (int i = 0; i < rl.size(); i++) {
				double cs = scoreWithoutNotLabel((RankList) rl.get(i));
				score += cs;
				out.write(((RankList) rl.get(i)).getID() + " " + cs + "\n");
				outQuery.write(((RankList) rl.get(i)).get(0).getDescription() + " " + cs + "\n");
			}
			outQuery.close();
			out.close();
		} catch (Exception e) {
			return -1.0D;
		}
		return score / rl.size();
	}

	public double scoreERR3(RankList rl) {
		return 0.0D;
	}

	public double scoreWithNotLabel(RankList rl) {
		return 0.0D;
	}

	public double scoreWithoutNotLabel(RankList rl) {
		return 0.0D;
	}

	public double score(RankList rl) {
		System.out.println("--MetricScore--");
		return 0.0D;
	}

	public MetricScorer clone() {
		return null;
	}

	public String name() {
		return "";
	}

	public double[][] swapChange(RankList rl) {
		return (double[][]) null;
	}

	public double[][] swapChange2(RankList rl) {
		return (double[][]) null;
	}

	public double[][] swapChange3(RankList rl) {
		return (double[][]) null;
	}
}