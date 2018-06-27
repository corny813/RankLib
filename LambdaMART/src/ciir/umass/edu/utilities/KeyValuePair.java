package ciir.umass.edu.utilities;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class KeyValuePair {
	protected List<String> keys = new ArrayList();
	protected List<String> values = new ArrayList();

	public KeyValuePair(String text) {
		try {
			int idx = text.lastIndexOf("#");
			if (idx != -1) {
				text = text.substring(0, idx).trim();
			}
			String[] fs = text.split(" ");
			for (int i = 0; i < fs.length; i++) {
				fs[i] = fs[i].trim();
				if (fs[i].compareTo("") == 0)
					continue;
				this.keys.add(getKey(fs[i]));
				this.values.add(getValue(fs[i]));
			}

		} catch (Exception ex) {
			System.out.println("Error in KeyValuePair(text) constructor");
		}
	}

	public List<String> keys() {
		return this.keys;
	}

	public List<String> values() {
		return this.values;
	}

	private String getKey(String pair) {
		return pair.substring(0, pair.indexOf(":"));
	}

	private String getValue(String pair) {
		return pair.substring(pair.lastIndexOf(":") + 1);
	}
}