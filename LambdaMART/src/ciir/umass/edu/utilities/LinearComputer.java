package ciir.umass.edu.utilities;

import java.util.Enumeration;
import java.util.Hashtable;

public class LinearComputer {
	Hashtable<String, Float> map = new Hashtable();

	public LinearComputer(String name, String description) {
		String[] strs = description.split(" ");
		for (int i = 0; i < strs.length; i++) {
			strs[i] = strs[i].trim();
			if (strs[i].compareTo("") == 0)
				continue;
			String key = strs[i].substring(0, strs[i].indexOf(":"));
			float value = Float.parseFloat(strs[i].substring(strs[i].lastIndexOf(":") + 1));
			this.map.put(key, Float.valueOf(value));
		}
	}

	public float compute(float[] featureList) {
		float output = 0.0F;
		for (Enumeration e = this.map.keys(); e.hasMoreElements();) {
			String key = ((String) e.nextElement()).toString();
			float weight = ((Float) this.map.get(key)).floatValue();
			int d = Integer.parseInt(key);
			float fVal = 0.0F;
			if (d < featureList.length)
				fVal = featureList[d];
			output += weight * fVal;
		}
		return output;
	}

	public String toString() {
		String output = "";
		for (Enumeration e = this.map.keys(); e.hasMoreElements();) {
			String key = ((String) e.nextElement()).toString();
			float weight = ((Float) this.map.get(key)).floatValue();
			output = output + key + ":" + weight + " ";
		}
		return output.trim();
	}

	public int size() {
		return this.map.size();
	}
}