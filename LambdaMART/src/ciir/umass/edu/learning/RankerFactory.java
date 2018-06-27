package ciir.umass.edu.learning;

import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.LambdaRank;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.MART;
import ciir.umass.edu.learning.tree.RFRanker;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.List;

public class RankerFactory {
	protected Ranker[] rFactory = { new MART(), new RankBoost(), new RankNet(), new AdaRank(), new CoorAscent(),
			new LambdaRank(), new LambdaMART(), new ListNet(), new RFRanker() };
	protected static Hashtable<String, RANKER_TYPE> map = new Hashtable();

	public RankerFactory() {
		map.put(createRanker(RANKER_TYPE.MART).name().toUpperCase(), RANKER_TYPE.MART);
		map.put(createRanker(RANKER_TYPE.RANKNET).name().toUpperCase(), RANKER_TYPE.RANKNET);
		map.put(createRanker(RANKER_TYPE.RANKBOOST).name().toUpperCase(), RANKER_TYPE.RANKBOOST);
		map.put(createRanker(RANKER_TYPE.ADARANK).name().toUpperCase(), RANKER_TYPE.ADARANK);
		map.put(createRanker(RANKER_TYPE.COOR_ASCENT).name().toUpperCase(), RANKER_TYPE.COOR_ASCENT);
		map.put(createRanker(RANKER_TYPE.LAMBDARANK).name().toUpperCase(), RANKER_TYPE.LAMBDARANK);
		map.put(createRanker(RANKER_TYPE.LAMBDAMART).name().toUpperCase(), RANKER_TYPE.LAMBDAMART);
		map.put(createRanker(RANKER_TYPE.LISTNET).name().toUpperCase(), RANKER_TYPE.LISTNET);
		map.put(createRanker(RANKER_TYPE.RANDOM_FOREST).name().toUpperCase(), RANKER_TYPE.RANDOM_FOREST);
	}

	public Ranker createRanker(RANKER_TYPE type) {
		Ranker r = this.rFactory[(type.ordinal() - RANKER_TYPE.MART.ordinal())].clone();
		return r;
	}

	public Ranker createRanker(RANKER_TYPE type, List<RankList> samples, int[] features) {
		Ranker r = createRanker(type);
		r.set(samples, features);
		return r;
	}

	public Ranker loadRanker(String modelFile) {
		Ranker r = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), "ASCII"));
			String content = in.readLine();
			in.close();
			content = content.replace("## ", "").trim();
			System.out.println("Model:\t\t" + content);
			r = createRanker((RANKER_TYPE) map.get(content.toUpperCase()));
			System.out.println("createRanker done");
			r.load(modelFile);
			System.out.println("load done");
		} catch (Exception ex) {
			System.out.println("Error in RankerFactory.load(): " + ex.toString());
		}
		return r;
	}
}