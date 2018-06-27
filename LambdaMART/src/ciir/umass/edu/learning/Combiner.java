package ciir.umass.edu.learning;

import ciir.umass.edu.learning.tree.Ensemble;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.utilities.FileUtils;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

public class Combiner {
	public static void main(String[] args) {
		Combiner c = new Combiner();
		c.combine(args[0], args[1]);
	}

	public void combine(String directory, String outputFile) {
		RankerFactory rf = new RankerFactory();
		String[] fns = FileUtils.getAllFiles(directory);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "ASCII"));
			out.write("## " + new RFRanker().name() + "\n");
			for (int i = 0; i < fns.length; i++) {
				if (fns[i].indexOf(".progress") != -1)
					continue;
				String fn = directory + fns[i];
				RFRanker r = (RFRanker) rf.loadRanker(fn);
				Ensemble en = r.getEnsembles()[0];
				out.write(en.toString());
			}
			out.close();
		} catch (Exception e) {
			System.out.println("Error in Combiner::combine(): " + e.toString());
		}
	}
}