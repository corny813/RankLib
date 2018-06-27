package ciir.umass.edu.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileUtils {
	public static final int BUF_SIZE = 51200;

	public static String read(String filename, String encoding) {
		String content = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));

			char[] newContent = new char[40960];
			int numRead = -1;
			while ((numRead = in.read(newContent)) != -1) {
				content = content + new String(newContent, 0, numRead);
			}
			in.close();
		} catch (Exception e) {
			content = "";
		}
		return content;
	}

	public static List<String> readLine(String filename, String encoding) {
		List lines = new ArrayList();
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));

			while ((content = in.readLine()) != null) {
				content = content.trim();
				if (content.length() == 0)
					continue;
				lines.add(content);
			}
			in.close();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
		return lines;
	}

	public static boolean write(String filename, String encoding, String strToWrite) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), encoding));

			out.write(strToWrite);
			out.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static String[] getAllFiles(String directory) {
		File dir = new File(directory);
		String[] fns = dir.list();
		return fns;
	}

	public static List<String> getAllFiles2(String directory) {
		File dir = new File(directory);
		String[] fns = dir.list();
		List<String> files = new ArrayList<String>();
		if (fns != null)
			for (int i = 0; i < fns.length; i++)
				files.add(fns[i]);
		return files;
	}

	public static boolean exists(String file) {
		File f = new File(file);
		return f.exists();
	}

	public static void copyFile(String srcFile, String dstFile) {
		try {
			FileInputStream fis = new FileInputStream(new File(srcFile));
			FileOutputStream fos = new FileOutputStream(new File(dstFile));
			try {
				byte[] buf = new byte[40960];
				int i = 0;
				while ((i = fis.read(buf)) != -1) {
					fos.write(buf, 0, i);
				}
			} catch (Exception e) {
				System.out.println("Error in FileUtils.copyFile: " + e.toString());
			} finally {
				if (fis != null)
					fis.close();
				if (fos != null)
					fos.close();
			}
		} catch (Exception ex) {
			System.out.println("Error in FileUtils.copyFile: " + ex.toString());
		}
	}

	public static void copyFiles(String srcDir, String dstDir, List<String> files) {
		for (int i = 0; i < files.size(); i++)
			copyFile(srcDir + (String) files.get(i), dstDir + (String) files.get(i));
	}

	public static int gunzipFile(File file_input, File dir_output) {

		try {
			FileInputStream in = new FileInputStream(file_input);
			BufferedInputStream source = new BufferedInputStream(in);
			GZIPInputStream gzip_in_stream = new GZIPInputStream(source);

			String file_input_name = file_input.getName();
			String file_output_name = file_input_name.substring(0, file_input_name.length() - 3);

			File output_file = new File(dir_output, file_output_name);

			byte[] input_buffer = new byte[51200];
			int len = 0;

			FileOutputStream out = new FileOutputStream(output_file);
			BufferedOutputStream destination = new BufferedOutputStream(out, 51200);

			while ((len = gzip_in_stream.read(input_buffer, 0, 51200)) != -1)
				destination.write(input_buffer, 0, len);
			destination.flush();
			out.close();
			gzip_in_stream.close();
		} catch (IOException e) {
			return 0;
		}
		return 1;
	}

	public static int gzipFile(String inputFile, String gzipFilename) {
		try {
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzipFilename));

			FileInputStream in = new FileInputStream(inputFile);

			byte[] buf = new byte[51200];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();

			out.finish();
			out.close();
		} catch (Exception ex) {
			return 0;
		}
		return 1;
	}
}