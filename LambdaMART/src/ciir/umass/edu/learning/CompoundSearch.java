package ciir.umass.edu.learning;

import ciir.umass.edu.utilities.MyThreadPool;
import java.io.PrintStream;
import java.util.HashSet;

public class CompoundSearch {
	private String initFeaturePath;
	private String fullFeaturePath;
	private Trainer initTrainer;
	private Trainer trainer;
	private int minNodeCount = 30;
	private int maxNodeCount = 100;
	private int tryTimes = 50;
	private Node initNode;
	private Node fullNode;

	public CompoundSearch(String initFeaturePath, String fullFeaturePath, Trainer initTrainer, Trainer trainer) {
		this.initFeaturePath = initFeaturePath;
		this.fullFeaturePath = fullFeaturePath;
		this.initTrainer = initTrainer;
		this.trainer = trainer;
		this.initNode = new Node(initFeaturePath);
		this.fullNode = new Node(fullFeaturePath);
	}

	public void work() {
		MyThreadPool.init(Runtime.getRuntime().availableProcessors());
		System.out.println("availableProcessors " + Runtime.getRuntime().availableProcessors());
		double bestScore = this.initTrainer.score();
		System.out.println("initScore " + bestScore);
		int count = 0;
		HashSet visitedNodes = new HashSet();
		int index = 0;
		int step = 0;
		while (true) {
			step++;
			System.out.println("step " + step);
			SearchAdjacentNode searchAdjacentNode = new SearchAdjacentNode(this.initNode, this.fullNode,
					this.minNodeCount, this.maxNodeCount, this.trainer, visitedNodes);
			Node compoundNode = searchAdjacentNode.work();
			System.out.println("visitedNodes " + visitedNodes.size());
			if (compoundNode == null) {
				System.out.println("compoundNode == null");
				break;
			}
			this.trainer.train(compoundNode);
			double score = this.trainer.score();
			visitedNodes.add(compoundNode);
			if (score > bestScore) {
				System.out.println("bestScore " + score + " > " + bestScore);
				index++;
				bestScore = score;
				count = 0;
				this.trainer.save(index);
			} else {
				count++;
			}

			if (count > this.tryTimes) {
				System.out.println("count > tryTimes");
				break;
			}

			this.initNode = compoundNode;
		}

		MyThreadPool.getInstance().shutdown();
	}
}