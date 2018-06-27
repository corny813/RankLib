package ciir.umass.edu.learning;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import ciir.umass.edu.learning.Node;
import java.util.Set;
import java.util.TreeSet;

public class SearchAdjacentNode {
	private Node startNode;
	private Node fullNode;
	private int minNodeCount = 30;
	private int maxNodeCount = 100;
	private Trainer trainer;
	private Set<Node> visitedNodes;
	private int compoundCount = 2;

	private TreeSet<Node> sortedNodes = new TreeSet();

	public SearchAdjacentNode(Node startNode, Node fullNode, int minNodeCount, int maxNodeCount, Trainer trainer,
			Set<Node> visitedNodes) {
		this.startNode = startNode;
		this.fullNode = fullNode;
		this.minNodeCount = minNodeCount;
		this.maxNodeCount = maxNodeCount;
		this.trainer = trainer;
		this.visitedNodes = visitedNodes;
	}

	public Node work() {
		System.out.println(this.startNode.size());

		List<Node> adjNodes = getAdjacentNode(this.startNode);

		System.out.println("loop start");

		for (Node node : adjNodes) {
			if (valid(node)) {
				this.trainer.train(node);
				double score = this.trainer.score();
				node.setScore(score);

				this.sortedNodes.add(node);
				mark(node);
			}
		}
		System.out.println("loop end");
		if (this.sortedNodes.size() < this.compoundCount) {
			return null;
		}

		Node compoundNode = compound();

		return compoundNode;
	}

	private boolean valid(Node node) {
		if (this.visitedNodes.contains(node)) {
			return false;
		}

		return (node.size() <= this.maxNodeCount) && (node.size() >= this.minNodeCount);
	}

	private void mark(Node node) {
		this.visitedNodes.add(node);
	}

	private Node compound(Node startNode, Node top1, Node top2) {
		boolean add1 = false;
		boolean add2 = false;
		if (startNode.size() < top1.size()) {
			add1 = true;
		}
		if (startNode.size() < top2.size()) {
			add2 = true;
		}

		int feature1 = startNode.substract(top1);
		Node resultNode;
		if (add1) {
			resultNode = startNode.add(feature1);
		} else {
			resultNode = startNode.remove(feature1);
		}

		int feature2 = startNode.substract(top2);
		if (add2) {
			resultNode = resultNode.add(feature2);
		} else {
			resultNode = resultNode.remove(feature2);
		}
		return resultNode;
	}

	private List<Node> getAdjacentNode(Node node) {
		List list = new ArrayList();
		Set set = node.getSet();
		for (Iterator localIterator = set.iterator(); localIterator.hasNext();) {
			Integer feature = (Integer) localIterator.next();
			Node removedNode = node.remove(feature.intValue());
			list.add(removedNode);
		}
		Set<Integer> fullSet = this.fullNode.getSet();
		((Set) fullSet).removeAll(set);
		for (Integer feature : fullSet) {
			Node addedNode = node.add(feature.intValue());
			list.add(addedNode);
		}
		return (List<Node>) list;
	}

	private Node compound() {
		Node resultNode = this.startNode;
		Iterator iterator = this.sortedNodes.iterator();
		Set<Integer> addSet = new HashSet();
		Set<Integer> removeSet = new HashSet();
		Node node;
		while (this.compoundCount > 0) {
			node = (Node) iterator.next();
			int feature = this.startNode.substract(node);

			if (this.startNode.size() > node.size()) {
				removeSet.add(Integer.valueOf(feature));
			} else {
				addSet.add(Integer.valueOf(feature));
			}
			this.compoundCount -= 1;
		}
		for (Integer feature : removeSet) {
			System.out.println("remove " + feature);
			resultNode = resultNode.remove(feature.intValue());
		}
		for (Integer feature : addSet) {
			System.out.println("add " + feature);
			resultNode = resultNode.add(feature.intValue());
		}

		System.out.println("CompoundNode" + resultNode);

		return resultNode;
	}
}