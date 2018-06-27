package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.DataPoint;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class Ensemble {
	protected List<RegressionTree> trees = null;
	protected List<Float> weights = null;

	public Ensemble() {
		this.trees = new ArrayList();
		this.weights = new ArrayList();
	}

	public Ensemble(Ensemble e) {
		this.trees = new ArrayList();
		this.weights = new ArrayList();
		this.trees.addAll(e.trees);
		this.weights.addAll(e.weights);
	}

	public Ensemble(String xmlRep) {
		try {
			this.trees = new ArrayList();
			this.weights = new ArrayList();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			byte[] xmlDATA = xmlRep.getBytes();
			ByteArrayInputStream in = new ByteArrayInputStream(xmlDATA);
			Document doc = dBuilder.parse(in);
			NodeList nl = doc.getElementsByTagName("tree");
			for (int i = 0; i < nl.getLength(); i++) {
				org.w3c.dom.Node n = nl.item(i);

				Split root = create(n.getFirstChild());

				float weight = Float.parseFloat(n.getAttributes().getNamedItem("weight").getNodeValue().toString());

				this.trees.add(new RegressionTree(root));
				this.weights.add(Float.valueOf(weight));
			}
		} catch (Exception ex) {
			System.out.println("Error in Emsemble(xmlRepresentation): " + ex.toString());
		}
	}

	public void chectFeatureValue(TreeMap<Integer, Float> minFeatureValue, TreeMap<Integer, Float> maxFeatureValue) {
		for (int i = 0; i < this.trees.size(); i++) {
			Split root = ((RegressionTree) this.trees.get(i)).root;
			findMinMaxValue(root, minFeatureValue, maxFeatureValue);
		}
	}

	public void findMinMaxValue(Split root, TreeMap<Integer, Float> minFeatureValue,
			TreeMap<Integer, Float> maxFeatureValue) {
		if (root.getFeatureID() == -1) {
			return;
		}
		int featureID = root.getFeatureID();
		float thr = root.getThreshold();
		if (minFeatureValue.get(Integer.valueOf(featureID)) == null) {
			minFeatureValue.put(Integer.valueOf(featureID), Float.valueOf(thr));
		} else if (((Float) minFeatureValue.get(Integer.valueOf(featureID))).floatValue() > thr) {
			minFeatureValue.replace(Integer.valueOf(featureID), Float.valueOf(thr));
		}

		if (maxFeatureValue.get(Integer.valueOf(featureID)) == null) {
			maxFeatureValue.put(Integer.valueOf(featureID), Float.valueOf(thr));
		} else if (((Float) maxFeatureValue.get(Integer.valueOf(featureID))).floatValue() < thr) {
			maxFeatureValue.replace(Integer.valueOf(featureID), Float.valueOf(thr));
		}

		findMinMaxValue(root.getLeft(), minFeatureValue, maxFeatureValue);
		findMinMaxValue(root.getRight(), minFeatureValue, maxFeatureValue);
	}

	public void add(RegressionTree tree, float weight) {
		this.trees.add(tree);
		this.weights.add(Float.valueOf(weight));
	}

	public RegressionTree getTree(int k) {
		return (RegressionTree) this.trees.get(k);
	}

	public float getWeight(int k) {
		return ((Float) this.weights.get(k)).floatValue();
	}

	public double variance() {
		double var = 0.0D;
		for (int i = 0; i < this.trees.size(); i++) {
			var += ((RegressionTree) this.trees.get(i)).variance();
		}
		return var;
	}

	public void remove(int k) {
		this.trees.remove(k);
		this.weights.remove(k);
	}

	public int treeCount() {
		return this.trees.size();
	}

	public int leafCount() {
		int count = 0;
		for (int i = 0; i < this.trees.size(); i++) {
			count += ((RegressionTree) this.trees.get(i)).leaves().size();
		}
		return count;
	}

	public float eval(DataPoint dp) {
		float s = 0.0F;
		for (int i = 0; i < this.trees.size(); i++) {
			float v = ((RegressionTree) this.trees.get(i)).eval(dp) * ((Float) this.weights.get(i)).floatValue();

			s += v;
		}

		return s;
	}

	public String toString() {
		String strRep = "<ensemble>\n";
		for (int i = 0; i < this.trees.size(); i++) {
			strRep = strRep + "\t<tree id=\"" + (i + 1) + "\" weight=\"" + this.weights.get(i) + "\">\n";
			strRep = strRep + ((RegressionTree) this.trees.get(i)).toString("\t\t");
			strRep = strRep + "\t</tree>\n";
		}
		strRep = strRep + "</ensemble>\n";
		return strRep;
	}

	private Split create2(org.w3c.dom.Node n) {
		Split s = null;
		if (n.getFirstChild().getNodeName().compareToIgnoreCase("feature") == 0) {
			NodeList nl = n.getChildNodes();
			int fid = Integer.parseInt(nl.item(0).getFirstChild().getNodeValue().toString().trim());
			float threshold = Float.parseFloat(nl.item(1).getFirstChild().getNodeValue().toString().trim());
			s = new Split(fid, threshold, 0.0F);
			s.setLeft(create(nl.item(2)));
			s.setRight(create(nl.item(3)));
		} else {
			float output = Float.parseFloat(n.getFirstChild().getFirstChild().getNodeValue().toString().trim());
			s = new Split();
			s.setOutput(output);
		}
		return s;
	}

	private Split create(org.w3c.dom.Node n) {
		Split s = null;

		int fid = 0;
		float threshold = 0.0F;
		Split leftSplit = null;
		Split rightSplit = null;
		float output = 0.0F;

		boolean isLeaf = false;
		boolean isLeft = true;

		NodeList nodes = n.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			org.w3c.dom.Node node = nodes.item(i);
			String nodeName = node.getNodeName();
			if (nodeName.compareToIgnoreCase("feature") == 0) {
				fid = Integer.parseInt(node.getFirstChild().getNodeValue().toString().trim());
				if (ciir.umass.edu.learning.Node.featureidMap.containsKey(Integer.valueOf(fid))) {
					fid = ((Integer) ciir.umass.edu.learning.Node.featureidMap.get(Integer.valueOf(fid))).intValue();
				} else {
					System.out.println("cs.fid\t" + fid);
				}
			} else if (nodeName.compareToIgnoreCase("threshold") == 0) {
				threshold = Float.parseFloat(node.getFirstChild().getNodeValue().toString().trim());
			} else if (nodeName.compareToIgnoreCase("split") == 0) {
				if (isLeft) {
					leftSplit = create(node);
					isLeft = false;
				} else {
					rightSplit = create(node);
				}
			} else if (nodeName.compareToIgnoreCase("output") == 0) {
				output = Float.parseFloat(node.getFirstChild().getNodeValue().toString().trim());
				isLeaf = true;
			}
		}
		if (isLeaf) {
			s = new Split();
			s.setOutput(output);
		} else {
			s = new Split(fid, threshold, 0.0F);
			s.setLeft(leftSplit);
			s.setRight(rightSplit);
		}
		return s;
	}
}