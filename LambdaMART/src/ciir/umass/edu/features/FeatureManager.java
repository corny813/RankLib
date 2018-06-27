package ciir.umass.edu.features;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class FeatureManager {
	private Hashtable<String, Integer> featureMap = new Hashtable();
	private String[] fnames = null;

	public FeatureManager() {
		this.featureMap.put("IDF-SUM", Integer.valueOf(1));
		this.featureMap.put("IDF-STD", Integer.valueOf(2));
		this.featureMap.put("IDF-MMRATIO", Integer.valueOf(3));
		this.featureMap.put("IDF-MAX", Integer.valueOf(4));
		this.featureMap.put("IDF-MEAN", Integer.valueOf(5));
		this.featureMap.put("IDF-GEOMEAN", Integer.valueOf(6));
		this.featureMap.put("IDF-HARMEAN", Integer.valueOf(7));
		this.featureMap.put("IDF-STDMEANRATIO", Integer.valueOf(8));

		this.featureMap.put("SCQ-SUM", Integer.valueOf(9));
		this.featureMap.put("SCQ-STD", Integer.valueOf(10));
		this.featureMap.put("SCQ-MMRATIO", Integer.valueOf(11));
		this.featureMap.put("SCQ-MAX", Integer.valueOf(12));
		this.featureMap.put("SCQ-MEAN", Integer.valueOf(13));
		this.featureMap.put("SCQ-GEOMEAN", Integer.valueOf(14));
		this.featureMap.put("SCQ-HARMEAN", Integer.valueOf(15));
		this.featureMap.put("SCQ-STDMEANRATIO", Integer.valueOf(16));

		this.featureMap.put("ICTF-SUM", Integer.valueOf(17));
		this.featureMap.put("ICTF-STD", Integer.valueOf(18));
		this.featureMap.put("ICTF-MMRATIO", Integer.valueOf(19));
		this.featureMap.put("ICTF-MAX", Integer.valueOf(20));
		this.featureMap.put("ICTF-MEAN", Integer.valueOf(21));
		this.featureMap.put("ICTF-GEOMEAN", Integer.valueOf(22));
		this.featureMap.put("ICTF-HARMEAN", Integer.valueOf(23));
		this.featureMap.put("ICTF-STDMEANRATIO", Integer.valueOf(24));

		this.featureMap.put("SIM-CLARITY", Integer.valueOf(25));

		this.featureMap.put("QSCOPE", Integer.valueOf(26));
		this.featureMap.put("MI", Integer.valueOf(27));

		this.featureMap.put("CLARITY-5", Integer.valueOf(28));
		this.featureMap.put("CLARITY-10", Integer.valueOf(29));
		this.featureMap.put("CLARITY-50", Integer.valueOf(30));
		this.featureMap.put("CLARITY-100", Integer.valueOf(31));
		this.featureMap.put("CLARITY-500", Integer.valueOf(32));

		this.featureMap.put("QF-5", Integer.valueOf(33));
		this.featureMap.put("QF-10", Integer.valueOf(34));
		this.featureMap.put("QF-50", Integer.valueOf(35));
		this.featureMap.put("QF-100", Integer.valueOf(36));

		this.featureMap.put("WIG-5", Integer.valueOf(37));
		this.featureMap.put("WIG-10", Integer.valueOf(38));
		this.featureMap.put("WIG-50", Integer.valueOf(39));
		this.featureMap.put("WIG-100", Integer.valueOf(40));
		this.featureMap.put("WIG-500", Integer.valueOf(41));
	}

	public List<RankList> read(String fn) {
		return read(fn, false, false);
	}

	public List<RankList> read(String fn, boolean letor, boolean mustHaveRelDoc) {
		List samples = new ArrayList();
		Hashtable ht = new Hashtable();
		int countRL = 0;
		int countEntries = 0;
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "GBK"));

			String lastID = "";

			boolean hasRel = false;
			RankList rl = new RankList();
			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("#") == 0)) {
					continue;
				}
				if (countEntries % 100000 == 0) {
					System.out.print("\nReading feature file [" + fn + "]: " + countEntries + "... ");
					System.gc();
					System.runFinalization();
				}

				DataPoint qp = new DataPoint(content);
				if ((lastID.compareTo("") != 0) && (lastID.compareTo(qp.getID()) != 0)) {
					if ((!mustHaveRelDoc) || (hasRel))
						samples.add(rl);
					rl = new RankList();
					hasRel = false;
				}

				if ((letor) && (qp.getLabel() == 2.0F))
					qp.setLabel(3.0F);
				if (qp.getLabel() > 0.0F)
					hasRel = true;
				lastID = qp.getID();
				rl.add(qp);
				countEntries++;
			}

			if ((rl.size() > 0) && ((!mustHaveRelDoc) || (hasRel)))
				samples.add(rl);
			in.close();
			System.out.println("\nReading feature file [" + fn + "]... [Done.]            ");
			System.out.println("(" + samples.size() + " ranked lists, " + countEntries + " entries read)");
		} catch (Exception ex) {
			System.out.println("Error in FeatureManager::read(): " + ex.toString());
		}
		return samples;
	}

	public List<RankList> read2(String fn, boolean letor) {
		List samples = new ArrayList();
		Hashtable ht = new Hashtable();
		int countRL = 0;
		int countEntries = 0;
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));

			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("#") == 0)) {
					continue;
				}
				if (countEntries % 10000 == 0) {
					System.out.print("\rReading feature file [" + fn + "]: " + countRL + "... ");
				}
				DataPoint qp = new DataPoint(content);
				RankList rl = null;
				if (ht.get(qp.getID()) == null) {
					rl = new RankList();

					ht.put(qp.getID(), Integer.valueOf(samples.size()));
					samples.add(rl);
					countRL++;
				} else {
					rl = (RankList) samples.get(((Integer) ht.get(qp.getID())).intValue());
				}
				if ((letor) && (qp.getLabel() == 2.0F)) {
					qp.setLabel(3.0F);
				}
				rl.add(qp);

				countEntries++;
			}
			in.close();
			System.out.println("\rReading feature file [" + fn + "]... [Done.]            ");
			System.out.println("(" + samples.size() + " ranked lists, " + countEntries + " entries read)");
		} catch (Exception ex) {
			System.out.println("Error in FeatureManager::read(): " + ex.toString());
		}
		return samples;
	}

	public int getFeatureID(String fname) {
		return ((Integer) this.featureMap.get(fname)).intValue();
	}

	public String getFeatureName(int fid) {
		return this.fnames[fid];
	}

	public List<String> getFeatureNameFromFile(String fn) {
		List fName = new ArrayList();
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "ASCII"));

			while ((content = in.readLine()) != null) {
				content = content.trim();
				if ((content.length() == 0) || (content.indexOf("#") == 0))
					continue;
				fName.add(content);
			}
			in.close();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
		return fName;
	}

	public int[] getFeatureIDFromFile(String fn) {
		if (fn.compareTo("") == 0)
			return null;
		List l = getFeatureNameFromFile(fn);
		int[] fv = new int[l.size()];
		for (int i = 0; i < l.size(); i++)
			fv[i] = Integer.parseInt((String) l.get(i));
		return fv;
	}
}