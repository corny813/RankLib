package ciir.umass.edu.learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Sampler {
	protected List<RankList> samples = null;
	protected List<RankList> remains = null;

	public List<RankList> doSampling(List<RankList> samplingPool, float samplingRate, boolean withReplacement) {
		Random r = new Random();
		this.samples = new ArrayList();
		int size = (int) (samplingRate * samplingPool.size());
		if (withReplacement) {
			int[] used = new int[samplingPool.size()];
			Arrays.fill(used, 0);
			for (int i = 0; i < size; i++) {
				int selected = r.nextInt(samplingPool.size());
				this.samples.add(samplingPool.get(selected));
				used[selected] = 1;
			}
			this.remains = new ArrayList();
			for (int i = 0; i < samplingPool.size(); i++)
				if (used[i] == 0)
					this.remains.add(samplingPool.get(i));
		} else {
			List l = new ArrayList();
			for (int i = 0; i < samplingPool.size(); i++)
				l.add(Integer.valueOf(i));
			for (int i = 0; i < size; i++) {
				int selected = r.nextInt(l.size());
				this.samples.add(samplingPool.get(((Integer) l.get(selected)).intValue()));
				l.remove(selected);
			}
			this.remains = new ArrayList();
			for (int i = 0; i < l.size(); i++)
				this.remains.add(samplingPool.get(((Integer) l.get(i)).intValue()));
		}
		return this.samples;
	}

	public List<RankList> getSamples() {
		return this.samples;
	}

	public List<RankList> getRemains() {
		return this.remains;
	}
}