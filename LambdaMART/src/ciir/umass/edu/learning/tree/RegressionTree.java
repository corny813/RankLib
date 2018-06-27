package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Node;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class RegressionTree {
	protected int nodes = 10;
	protected int minLeafSupport = 1;

	protected Split root = null;
	protected List<Split> leaves = null;

	protected DataPoint[] trainingSamples = null;
	protected float[] trainingLabels = null;
	protected int[] features = null;
	protected float[][] thresholds = (float[][]) null;
	protected int[] index = null;
	protected FeatureHistogram hist = null;
	protected ExcludeSortedDoc excludeSortedDoc = null;
	public int[] featureUseRate = null;

	public static Comparator<Split> splitGainComparator = new Comparator<Split>() {
		public int compare(Split s1, Split s2) {
			float gain1 = s1.getDevRevenue();
			float gain2 = s2.getDevRevenue();
			if (gain1 > gain2)
				return -1;
			if (gain1 == gain2) {
				return 0;
			}
			return 1;
		}
	};

	public RegressionTree(Split root) {
		this.root = root;
		this.leaves = root.leaves();
	}

	public RegressionTree(int nLeaves, DataPoint[] trainingSamples, float[] labels, FeatureHistogram hist,
			int minLeafSupport) {
		this.nodes = nLeaves;
		this.trainingSamples = trainingSamples;
		this.trainingLabels = labels;
		this.hist = hist;
		this.minLeafSupport = minLeafSupport;
		this.index = new int[trainingSamples.length];
		for (int i = 0; i < trainingSamples.length; i++)
			this.index[i] = i;
	}

	public RegressionTree(int nLeaves, DataPoint[] trainingSamples, float[] labels, ExcludeSortedDoc excludeSortedDoc,
			FeatureHistogram hist, int minLeafSupport) {
		this.nodes = nLeaves;
		this.trainingSamples = trainingSamples;
		this.trainingLabels = labels;
		this.excludeSortedDoc = excludeSortedDoc;
		this.minLeafSupport = minLeafSupport;
		this.hist = hist;
		this.index = new int[trainingSamples.length];
		for (int i = 0; i < trainingSamples.length; i++)
			this.index[i] = i;
	}

	public void set(int nLeaves, DataPoint[] trainingSamples, float[] labels, FeatureHistogram hist,
			int minLeafSupport) {
		this.nodes = nLeaves;
		this.trainingSamples = trainingSamples;
		this.trainingLabels = labels;
		this.hist = hist;
		this.minLeafSupport = minLeafSupport;
		this.index = new int[trainingSamples.length];
		for (int i = 0; i < trainingSamples.length; i++)
			this.index[i] = i;
	}

	public void fit(double rate) {
		int newNodes = this.nodes;
		List queue = new ArrayList();
		this.root = split(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0));
		insert(queue, this.root.getLeft());
		insert(queue, this.root.getRight());
		this.root.clearSamples();
		int taken = 0;
		newNodes = (int) (this.nodes * rate);

		while (((newNodes == -1) || (taken + queue.size() < newNodes)) && (queue.size() > 0)) {
			Split leaf = (Split) queue.get(0);
			queue.remove(0);

			Split s = split(leaf);

			if (s == null) {
				taken++;
			} else {
				leaf.clearSamples();

				insert(queue, s.getLeft());
				insert(queue, s.getRight());
			}

		}

		this.leaves = this.root.leaves();
		System.out.println(this.leaves.size());
	}

	public void fit_splitselect(double rate) {
		int newNodes = this.nodes;
		newNodes = (int) (this.nodes * rate);

		PriorityQueue pq = new PriorityQueue(newNodes * 2, splitGainComparator);
		int taken = 0;
		this.root = split(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0));
		pq.add(this.root);
		while (((newNodes == -1) || (taken + pq.size() < newNodes)) && (pq.size() > 0)) {
			Split leaf = (Split) pq.poll();

			Split left_split = split(leaf.getLeft());
			if (left_split != null)
				pq.add(left_split);
			else {
				taken++;
			}
			Split right_split = split(leaf.getRight());
			if (right_split != null)
				pq.add(right_split);
			else {
				taken++;
			}
			leaf.clearSamples();
		}

		while (pq.size() > 0) {
			Split leaf = (Split) pq.poll();
			leaf.set(-1, 0.0F, leaf.getOriginDeviance(), -1);
			leaf.setLeft(null);
			leaf.setRight(null);
		}

		this.leaves = this.root.leaves();
		System.out.format("leaf num: %d\n", new Object[] { Integer.valueOf(this.leaves.size()) });
	}

	public void fit1(Split rooted) {
		List queue = new ArrayList();
		this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rooted);
		insert(queue, this.root.getLeft());
		insert(queue, this.root.getRight());
		this.root.clearSamples();
		int taken = 0;
		while (((this.nodes == -1) || (taken + queue.size() < this.nodes)) && (queue.size() > 0)) {
			Split leaf = (Split) queue.get(0);
			queue.remove(0);

			Split s = split(leaf);

			if (s == null) {
				taken++;
			} else {
				leaf.clearSamples();

				insert(queue, s.getLeft());
				insert(queue, s.getRight());
			}
		}

		this.leaves = this.root.leaves();
	}

	public void fit_mix() {
		List queue = new ArrayList();
		this.root = split(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0));
		System.out.println("root -------------- " + this.root.getDeviance() + " " + this.root.getLeft().getDeviance()
				+ " " + this.root.getRight().getDeviance());

		insert2(queue, this.root);

		int selectIndex = 0;
		while ((this.nodes == -1) || (queue.size() < this.nodes)) {
			selectIndex = selectSplitNodeIndex(queue, 10);

			Split leaf = (Split) queue.get(selectIndex);
			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(selectIndex);
			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
		this.leaves = this.root.leaves();
	}

	public void fit_with_existed_tree(double rate) {
		List queue = new ArrayList();
		List queueLeaf = new ArrayList();
		this.root.set(this.index, this.hist, 3.4028235E+38F, 0.0D, 0);
		this.root = splitExist(this.root, this.root);
		queueLeaf.add(this.root);
		int newNodes = (int) (this.nodes * rate);
		while (queueLeaf.size() != 0) {
			Split existNode = (Split) queueLeaf.get(0);
			existNode = splitExist(existNode, existNode);
			if (existNode.getLeft().getFeatureID() != -1)
				queueLeaf.add(existNode.getLeft());
			else {
				insert(queue, existNode.getLeft());
			}
			if (existNode.getRight().getFeatureID() != -1)
				queueLeaf.add(existNode.getRight());
			else {
				insert(queue, existNode.getRight());
			}
			queueLeaf.remove(0);
			existNode.clearSamples();
		}

		System.out.println(queue.size() + "new queue size");
		for (int i = 0; i < queue.size(); i++) {
			System.out.print(
					((Split) queue.get(i)).parent_output + ":" + ((Split) queue.get(i)).getSampleCnt() + " ----- ");
		}
		System.out.println();
		fitChildOrigin(queue, newNodes);
		System.out.println(queue.size());
		this.leaves = this.root.leaves();
	}

	public void fitWithSplitRate(Split rooted) {
		List queueLeft = new ArrayList();
		List queueRight = new ArrayList();
		this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rooted);
		insert(queueLeft, this.root.getLeft());
		insert(queueRight, this.root.getRight());

		this.root.clearSamples();
		float childRate = this.root.getLeft().getSampleCnt() * 1.0F
				/ (this.root.getLeft().getSampleCnt() + this.root.getRight().getSampleCnt() + 1);
		System.out.println(
				this.root.getLeft().getSampleCnt() + " " + this.root.getRight().getSampleCnt() + " " + childRate);
		fitChildOrigin(queueLeft, (int) (this.nodes * childRate));
		fitChildOrigin(queueRight, (int) (this.nodes * (1.0F - childRate)));
		this.leaves = this.root.leaves();
	}

	public void fitChildOrigin(List<Split> queue, int nodeNum) {
		int taken = 0;
		for (int i = 0; i < queue.size(); i++) {
			((Split) queue.get(i)).setIsSplit(0);
		}

		while (((nodeNum == -1) || (taken + queue.size() < nodeNum)) && (queue.size() > 0)) {
			Split leaf = (Split) queue.get(0);
			queue.remove(0);

			Split s = split(leaf);

			if (s == null) {
				taken++;
			} else {
				leaf.clearSamples();

				insert(queue, s.getLeft());
				s.getLeft().setIsSplit(1);
				insert(queue, s.getRight());
				s.getRight().setIsSplit(1);
			}
		}
	}

	public void fit2() {
		List queue = new ArrayList();
		this.root = split(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0));
		System.out.println("root -------------- " + this.root.getDeviance() + " " + this.root.getLeft().getDeviance()
				+ " " + this.root.getRight().getDeviance());

		insert2(queue, this.root);
		System.out.println(this.root.getDevRevenue());
		while ((this.nodes == -1) || (queue.size() < this.nodes)) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(0);
			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
		this.leaves = this.root.leaves();
		System.out.println();
	}

	public void fit3(Split rooted) {
		System.out.println("fit3");
		List queue = new ArrayList();
		List queueLeaf = new ArrayList();
		List existQueue = new ArrayList();
		this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rooted);
		this.root.clearSamples();
		if (rooted.getLeft().getFeatureID() != -1) {
			existQueue.add(rooted.getLeft());
			existQueue.add(this.root.getLeft());
		} else {
			insert(queueLeaf, this.root.getLeft());
		}
		if (rooted.getRight().getFeatureID() != -1) {
			existQueue.add(rooted.getRight());
			existQueue.add(this.root.getRight());
		} else {
			insert(queueLeaf, this.root.getRight());
		}

		while (existQueue.size() != 0) {
			Split existLeaf = (Split) existQueue.get(0);
			existQueue.remove(0);
			Split leaf = (Split) existQueue.get(0);
			existQueue.remove(0);

			Split s = splitExist(leaf, existLeaf);
			leaf.clearSamples();
			if (existLeaf.getLeft().getFeatureID() != -1) {
				existQueue.add(existLeaf.getLeft());
				existQueue.add(s.getLeft());
			} else {
				insert(queueLeaf, s.getLeft());
			}
			if (existLeaf.getRight().getFeatureID() != -1) {
				existQueue.add(existLeaf.getRight());
				existQueue.add(s.getRight());
			} else {
				insert(queueLeaf, s.getRight());
			}
		}
		for (int i = 0; i < queueLeaf.size(); i++) {
			Split leaf = (Split) queueLeaf.get(i);
			Split s = split(leaf);
			insert2(queue, s);
		}

		System.out.println("copy over" + queue.size());
		while ((this.nodes == -1) || (queue.size() < this.nodes)) {
			Split leaf = (Split) queue.get(0);
			System.out.println(leaf.getSamples().length + " " + leaf.getDeviance() + " " + leaf.getDevRevenue());

			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(0);

			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
		this.leaves = this.root.leaves();
		this.root.updateUseFeatureRate(this.featureUseRate);
		for (int i = 0; i < this.featureUseRate.length; i++) {
			System.out.print(Node.idfeatureMap.get(Integer.valueOf(i)) + " " + this.featureUseRate[i] + " ");
		}
		System.out.println();
	}

	public void fit4(Split rooted) {
		System.out.println("fit4");
		List queue = new ArrayList();
		List queueLeaf = new ArrayList();
		List existQueue = new ArrayList();
		this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rooted);
		this.root.clearSamples();
		if ((rooted.getLeft() != null) && (rooted.getLeft().getFeatureID() != -1)) {
			existQueue.add(rooted.getLeft());
			existQueue.add(this.root.getLeft());
		} else {
			insert(queueLeaf, this.root.getLeft());
		}
		if ((rooted.getRight() != null) && (rooted.getRight().getFeatureID() != -1)) {
			existQueue.add(rooted.getRight());
			existQueue.add(this.root.getRight());
		} else {
			insert(queueLeaf, this.root.getRight());
		}

		while (existQueue.size() != 0) {
			Split existLeaf = (Split) existQueue.get(0);
			existQueue.remove(0);
			Split leaf = (Split) existQueue.get(0);
			existQueue.remove(0);

			Split s = splitExist(leaf, existLeaf);
			leaf.clearSamples();
			if (existLeaf.getLeft().getFeatureID() != -1) {
				existQueue.add(existLeaf.getLeft());
				existQueue.add(s.getLeft());
			} else {
				System.out.println(existLeaf.getLeft().getAveSplitScore() + " aveSplitScore "
						+ existLeaf.getLeft().getSampleCnt());
				insert(queueLeaf, s.getLeft());
			}
			if (existLeaf.getRight().getFeatureID() != -1) {
				existQueue.add(existLeaf.getRight());
				existQueue.add(s.getRight());
			} else {
				System.out.println(existLeaf.getRight().getAveSplitScore() + " aveSplitScore "
						+ existLeaf.getRight().getSampleCnt());
				insert(queueLeaf, s.getRight());
			}
		}

		int[] sortIndex = new int[queueLeaf.size()];
		int selectSplitThreshold = queueLeaf.size() / 2;
		Arrays.fill(sortIndex, 0);
		for (int i = 0; i < queueLeaf.size(); i++) {
			float currentSplitScore = ((Split) queueLeaf.get(i)).getAveSplitScore();
			for (int j = 0; j < queueLeaf.size(); j++) {
				if (currentSplitScore > ((Split) queueLeaf.get(j)).getAveSplitScore()) {
					sortIndex[i] += 1;
				}
			}
			System.out.print(sortIndex[i] + " ");
		}
		System.out.println();

		for (int i = 0; i < queueLeaf.size(); i++) {
			Split leaf = (Split) queueLeaf.get(i);
			if (sortIndex[i] < 1)
				;
			Split s = split(leaf);
			insert2(queue, s);
		}

		System.out.println("copy over" + queue.size());
		while ((this.nodes == -1) || (queue.size() < this.nodes)) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(0);

			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
		this.leaves = this.root.leaves();
		this.root.updateUseFeatureRate(this.featureUseRate);
		for (int i = 0; i < this.featureUseRate.length; i++) {
			System.out.print(Node.idfeatureMap.get(Integer.valueOf(i)) + " " + this.featureUseRate[i] + " ");
		}
		System.out.println();
	}

	public void fit5(int num) {
		if (this.hist == null) {
			System.out.println("root 1 hist is null");
		}
		Split root1 = new Split(this.index, this.excludeSortedDoc, 3.4028235E+38F, 0.0D, 0);
		Split leafClass1 = getSplitBranch(root1, 0, 0.5F);
		Split leafClass2;
		Split root2;
		if (leafClass1 != null) {
			if (root1.getLeft().getFeatureID() == -2)
				root2 = preSplitExistRoot(root1, 0);
			else {
				root2 = preSplitExistRoot(root1, 1);
			}
			leafClass2 = getSplitBranch(root2, 1, 0.6F);
		} else {
			root2 = new Split(this.index, this.excludeSortedDoc, 3.4028235E+38F, 0.0D, 0);
			leafClass2 = getSplitBranch(root2, 1, 0.5F);
		}

		if (leafClass1 != null) {
			System.out.println(root1.leaves().size() + " ---- root1");
		}
		if (leafClass2 != null) {
			System.out.println(root2.leaves().size() + " ---- root1");
		}

		Split root3 = getUnclassRoot(leafClass1, leafClass2);

		if ((leafClass1 != null) && (leafClass2 != null)) {
			if (root1.getLeft().getFeatureID() == -2)
				root1.setLeft(root2);
			else {
				root1.setRight(root2);
			}

		}

		Split rootTmp = new Split();
		if (leafClass1 != null) {
			rootTmp = root1;
			System.out.println("root1");
		} else if (leafClass2 != null) {
			rootTmp = root2;
			System.out.println("root2");
		} else {
			this.root = root3;
		}
		List queueLeaf = new ArrayList();
		queueLeaf.add(root3);

		if ((root1 != null) || (root2 != null)) {
			List copyNode = new ArrayList();
			List copyToNode = new ArrayList();
			System.out.println(rootTmp.getFeatureID());

			System.out.println("SetRoot");
			this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rootTmp);
			if (rootTmp.getLeft().getFeatureID() == -2) {
				this.root.getLeft().clearSamples();
				this.root.setLeft(root3);
			} else {
				copyNode.add(rootTmp.getLeft());
				copyToNode.add(this.root.getLeft());
			}

			if (rootTmp.getRight().getFeatureID() == -2) {
				this.root.getRight().clearSamples();
				this.root.setRight(root3);
			} else {
				copyNode.add(rootTmp.getRight());
				copyToNode.add(this.root.getRight());
			}
			while (copyNode.size() != 0) {
				Split sp = (Split) copyNode.get(0);
				Split s = (Split) copyToNode.get(0);

				copyNode.remove(0);
				copyToNode.remove(0);

				if (sp.getFeatureID() == -1) {
					queueLeaf.add(s);
					continue;
				}

				s = splitExist(s, sp);
				s.clearSamples();
				if (sp.getLeft().getFeatureID() == -2) {
					s.getLeft().clearSamples();
					s.setLeft(root3);
				} else {
					copyNode.add(sp.getLeft());
					copyToNode.add(s.getLeft());
				}

				if (sp.getRight().getFeatureID() == -2) {
					s.getRight().clearSamples();
					s.setRight(root3);
				} else {
					copyNode.add(sp.getRight());
					copyToNode.add(s.getRight());
				}
			}
		}

		System.out.println(queueLeaf.size() + "   " + this.nodes);
		List queue = new ArrayList();
		System.out.println(root3.getFeatureID() + " ");
		for (int i = 0; i < queueLeaf.size(); i++) {
			Split leaf = (Split) queueLeaf.get(i);
			Split s = split(leaf);
			System.out.println(leaf.getSamples().length + " dev " + leaf.getDeviance());
			if (s == null) {
				continue;
			}
			System.out.println("root -------------- " + s.getDeviance() + " " + s.getLeft().getDeviance() + " "
					+ s.getRight().getDeviance());
			insert2(queue, s);
		}
		System.out.println(
				root3.samples.length + " root33333 " + Node.idfeatureMap.get(Integer.valueOf(root3.getFeatureID())));
		System.out.println(queue.size() + "   " + this.nodes);
		while ((this.nodes == -1) || (queue.size() < this.nodes)) {
			Split leaf = (Split) queue.get(0);
			System.out.println(leaf.getSamples().length + " " + leaf.getDeviance() + " " + leaf.getDevRevenue() + " "
					+ leaf.getFeatureID() + " " + leaf.getLeft().getSamples().length + " "
					+ leaf.getRight().getSamples().length);

			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(0);

			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}
		System.out.println(queue.size() + "   " + this.nodes);

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.SetOutMessage("");
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
		System.out.println(root3.leaves().size() + " ---- root3");
		this.leaves = this.root.leaves();
	}

	public void fit6(int[] featureUseRate, int num, Split rooted, float childRate) {
		System.out.println("fit6");
		List queueLeft = new ArrayList();
		List queueRight = new ArrayList();
		this.root = splitExist(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0), rooted);
		this.root.clearSamples();

		Split left = split(this.root.getLeft());
		insert2(queueLeft, left);
		Split Right = split(this.root.getRight());
		insert2(queueRight, Right);
		fitChild(queueLeft, (int) (this.nodes * childRate));
		fitChild(queueRight, (int) (this.nodes - this.nodes * childRate));

		this.leaves = this.root.leaves();
		this.root.updateUseFeatureRate(featureUseRate);
		for (int i = 0; i < featureUseRate.length; i++) {
			System.out.print(Node.idfeatureMap.get(Integer.valueOf(i)) + " " + featureUseRate[i] + " ");
		}
		System.out.println();
	}

	public void fitWithoutNoise(int[] noisePoint) {
		Split BaseRoot = split(new Split(this.index, this.hist, 3.4028235E+38F, 0.0D, 0));
		this.root = splitExcludeNoise(BaseRoot, noisePoint);
		BaseRoot.clearSamples();
		this.root = split(this.root);
		List queue = new ArrayList();
		insert2(queue, this.root);
		fitChild(queue, this.nodes);
		this.leaves = this.root.leaves();
		this.root.updateUseFeatureRate(this.featureUseRate);
		for (int i = 0; i < this.featureUseRate.length; i++) {
			System.out.print(Node.idfeatureMap.get(Integer.valueOf(i)) + " " + this.featureUseRate[i] + " ");
		}
		System.out.println();
	}

	public void fitChild(List<Split> queue, int nodeNum) {
		System.out.println("copy over" + queue.size());
		while ((this.nodes == -1) || (queue.size() < nodeNum)) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getDevRevenue() <= 0.0F) {
				break;
			}
			leaf.clearSamples();
			queue.remove(0);

			Split sl = split(leaf.getLeft());
			Split sr = split(leaf.getRight());

			insert2(queue, leaf.getLeft());
			insert2(queue, leaf.getRight());
		}

		while (queue.size() != 0) {
			Split leaf = (Split) queue.get(0);

			if (leaf.getFeatureID() != -1)
				;
			leaf.setFeatureID(-1);
			queue.remove(0);
		}
	}

	public Split getUnclassRoot(Split leafClass1, Split leafClass2) {
		int[] tmpIndex = new int[this.index.length];
		Arrays.fill(tmpIndex, 1);
		if (leafClass1 != null) {
			for (int i = 0; i < leafClass1.samples.length; i++) {
				tmpIndex[leafClass1.samples[i]] = 0;
			}
		}
		if (leafClass2 != null) {
			for (int i = 0; i < leafClass2.samples.length; i++) {
				tmpIndex[leafClass2.samples[i]] = 0;
			}
		}
		int count = 0;
		for (int i = 0; i < tmpIndex.length; i++) {
			count += tmpIndex[i];
		}
		int[] indexRoot = new int[count];
		int lr = 0;
		for (int i = 0; i < tmpIndex.length; i++) {
			if (tmpIndex[i] == 0) {
				continue;
			}
			indexRoot[(lr++)] = i;
		}
		tmpIndex = null;
		FeatureHistogram lh = new FeatureHistogram();
		lh.construct(this.hist, indexRoot, this.trainingLabels, indexRoot);
		if (this.hist == null) {
			System.out.println("unclass hist is null");
		}

		Split res = new Split(indexRoot, lh, 3.4028235E+38F, 0.0D, 0);
		return res;
	}

	public Split getSplitBranch(Split rootTmp, int tag, float preRate) {
		List sp = preSortedDoc(rootTmp, preRate, tag);
		if (sp.size() == 0) {
			rootTmp.clearSamples();
			return null;
		}

		rootTmp.clearSamples();
		System.out.println(Node.idfeatureMap.get(Integer.valueOf(rootTmp.getFeatureID())));
		Split tmpSplit = (Split) sp.get(0);
		while ((sp.size() > 0) && (preRate < 0.65D)) {
			preRate = (float) (preRate + 0.1D);
			if (preRate > 0.7F) {
				preRate = 0.7F;
			}
			sp = preSortedDoc(tmpSplit, preRate, tag);
			System.out.println(Node.idfeatureMap.get(Integer.valueOf(tmpSplit.getFeatureID())));
			if (sp.size() == 0) {
				tmpSplit.clearSamples();
				return null;
			}

			tmpSplit.clearSamples();
			tmpSplit = (Split) sp.get(0);
		}
		return tmpSplit;
	}

	protected List<Split> preSortedDoc(Split s, float accordanceRate, int tag) {
		ExcludeSortedDoc e = s.excludeSortedDoc;
		return e.findBestSplit(s, this.trainingSamples, accordanceRate, this.minLeafSupport, tag);
	}

	protected Split preSplitExistRoot(Split s, int tag) {
		ExcludeSortedDoc e = this.excludeSortedDoc;
		s.samples = this.index;
		s.excludeSortedDoc = this.excludeSortedDoc;
		return e.setChildNode(s, this.trainingSamples, tag);
	}

	protected Split split(Split s) {
		FeatureHistogram h = s.hist;
		return h.findBestSplit(s, this.trainingSamples, this.trainingLabels, this.minLeafSupport);
	}

	protected Split splitExcludeNoise(Split s, int[] noisePoint) {
		FeatureHistogram h = s.hist;
		return h.excludeNoiseSample(s, this.trainingSamples, this.trainingLabels, noisePoint);
	}

	protected Split splitExist(Split s, Split existsp) {
		FeatureHistogram h = s.hist;
		return h.setBestSplit(s, this.trainingSamples, this.trainingLabels, this.minLeafSupport, existsp);
	}

	public float eval(DataPoint dp) {
		return this.root.eval(dp);
	}

	public List<Split> leaves() {
		return this.leaves;
	}

	public void clearSamples() {
		this.trainingSamples = null;
		this.trainingLabels = null;
		this.features = null;
		this.thresholds = ((float[][]) null);
		this.index = null;
		this.hist = null;
		for (int i = 0; i < this.leaves.size(); i++) {
			((Split) this.leaves.get(i)).clearSamples();
			if (((Split) this.leaves.get(i)).getLeft() != null) {
				((Split) this.leaves.get(i)).getLeft().clearSamples();
			}
			if (((Split) this.leaves.get(i)).getRight() != null)
				((Split) this.leaves.get(i)).getRight().clearSamples();
		}
	}

	public String toString() {
		if (this.root != null)
			return this.root.toString();
		return "";
	}

	public String toString(String indent) {
		if (this.root != null)
			return this.root.toString(indent);
		return "";
	}

	public double variance() {
		double var = 0.0D;
		for (int i = 0; i < this.leaves.size(); i++)
			var += ((Split) this.leaves.get(i)).getDeviance();
		return var;
	}

	protected void insert(List<Split> ls, Split s) {
		int i = 0;
		while (i < ls.size()) {
			if (((Split) ls.get(i)).getDeviance() <= s.getDeviance())
				break;
			i++;
		}

		ls.add(i, s);
	}

	protected void insert2(List<Split> ls, Split s) {
		int i = 0;
		float dev = s.getDevRevenue();
		float tmpdev = 0.0F;
		while (i < ls.size()) {
			tmpdev = ((Split) ls.get(i)).getDevRevenue();
			if (tmpdev <= dev)
				break;
			i++;
		}

		ls.add(i, s);
	}

	protected int selectSplitNodeIndex(List<Split> ls, int originNum) {
		float[] nodeDev = new float[ls.size()];
		float[] nodeDevRev = new float[ls.size()];
		int[] index = new int[ls.size()];
		for (int i = 0; i < ls.size(); i++) {
			nodeDev[i] = ((Split) ls.get(i)).getOriginDeviance();
			nodeDevRev[i] = ((Split) ls.get(i)).getDevRevenue();
			index[i] = i;
		}

		for (int i = 0; i < ls.size(); i++) {
			for (int j = 0; j < ls.size() - 1; j++) {
				if (nodeDev[j] < nodeDev[(j + 1)]) {
					float tmp = nodeDev[j];
					nodeDev[j] = nodeDev[(j + 1)];
					nodeDev[(j + 1)] = tmp;
					tmp = nodeDevRev[j];
					nodeDevRev[j] = nodeDevRev[(j + 1)];
					nodeDevRev[(j + 1)] = tmp;
					int tmpId = index[j];
					index[j] = index[(j + 1)];
					index[(j + 1)] = tmpId;
				}
			}
		}
		float maxDevRev = nodeDevRev[0];

		int res = index[0];
		if (originNum > ls.size()) {
			originNum = ls.size();
		}
		for (int i = 0; i < originNum; i++) {
			if (maxDevRev < nodeDevRev[i]) {
				maxDevRev = nodeDevRev[i];
				res = index[i];
			}
		}

		return res;
	}

	class SplitWorker implements Runnable {
		RegressionTree tree = null;
		Split s = null;
		Split ret = null;

		SplitWorker(RegressionTree tree, Split s) {
			this.tree = tree;
			this.s = s;
		}

		public void run() {
			this.ret = RegressionTree.this.split(this.s);
		}
	}
}