package ciir.umass.edu.learning;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node implements Comparable<Node> {
	public static TreeMap<Integer, Integer> featureidMap = new TreeMap();
	public static TreeMap<Integer, Integer> idfeatureMap = new TreeMap();
	public static TreeMap<Integer, Integer> idfeaturetotalMap = new TreeMap();
	private String featuresPath;
	private HashSet<Integer> set = new HashSet();
	private int count;
	private double score;

	public double getScore() {
		return this.score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public Node(int count) {
		this.count = count;

		for (int i = 1; i < count; i++) {
			this.set.add(Integer.valueOf(i));
		}
	}

	public Node(HashSet<Integer> set) {
		this.set = set;
	}

	public HashSet<Integer> getSet() {
		return this.set;
	}

	public void setSet(HashSet<Integer> set) {
		this.set = set;
	}

	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Integer integer : this.set) {
			stringBuilder.append(integer).append(" ");
		}
		return stringBuilder.toString();
	}

	public Node add(int feature) {
		HashSet hashSet = new HashSet(this.set);
		hashSet.add(Integer.valueOf(feature));
		Node node = new Node(hashSet);
		return node;
	}

	public Node remove(int feature) {
		HashSet hashSet = new HashSet(this.set);
		hashSet.remove(Integer.valueOf(feature));
		Node node = new Node(hashSet);
		return node;
	}

	public int substract(Node node) {
		int diff = node.size() - size();
		if ((diff != 1) && (diff != -1)) {
			return -1;
		}
		int feature = -1;
		if (diff == 1) {
			HashSet hashSet = new HashSet(node.getSet());
			hashSet.removeAll(this.set);
			Iterator iterator = hashSet.iterator();
			feature = ((Integer) iterator.next()).intValue();
		} else {
			HashSet hashSet = new HashSet(this.set);
			hashSet.removeAll(node.getSet());
			Iterator iterator = hashSet.iterator();
			feature = ((Integer) iterator.next()).intValue();
		}
		return feature;
	}

	public Node(String featuresPath) {
		this.featuresPath = featuresPath;
		parseFeaturePath();
	}

	public Node() {
		for (int i = 2; i < 5; i++) {
			this.set.add(Integer.valueOf(i));
		}
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Node other = (Node) obj;
		return this.set.equals(other.getSet());
	}

	public int hashCode() {
		return this.set.hashCode();
	}

	int size() {
		return this.set.size();
	}

	private void parseFeaturePath() {
		try {
			Scanner scanner = new Scanner(new FileInputStream(this.featuresPath));
			int value = 1;
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				String content = line.trim();
				if ((content.isEmpty()) || (content.startsWith("#"))) {
					continue;
				}
				String[] ss = content.split("\\s+");
				this.set.add(Integer.valueOf(Integer.parseInt(ss[0])));
				featureidMap.put(Integer.valueOf(Integer.parseInt(ss[0])), Integer.valueOf(value));
				idfeatureMap.put(Integer.valueOf(value), Integer.valueOf(Integer.parseInt(ss[0])));
				value++;
			}

			scanner.close();

			int[] useFeature = { 1152, 1101, 1121, 1044, 1172, 1141, 1162, 1041, 5001, 1176 };
			for (int i = 0; i < useFeature.length; i++) {
				if (featureidMap.get(Integer.valueOf(useFeature[i])) != null)
					idfeaturetotalMap.put(featureidMap.get(Integer.valueOf(useFeature[i])), Integer.valueOf(1));
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public int compareTo(Node t) {
		return Double.compare(t.getScore(), getScore());
	}
}