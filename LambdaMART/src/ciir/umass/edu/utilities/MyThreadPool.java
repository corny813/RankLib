package ciir.umass.edu.utilities;

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyThreadPool extends ThreadPoolExecutor {
	private final Semaphore semaphore;
	private int size = 0;

	private static MyThreadPool singleton = null;

	private MyThreadPool(int size) {
		super(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
		this.semaphore = new Semaphore(size, true);
		this.size = size;
	}

	public static MyThreadPool getInstance() {
		return singleton;
	}

	public static void init(int poolSize) {
		singleton = new MyThreadPool(poolSize);
	}

	public int size() {
		return this.size;
	}

	public void await() {
		for (int i = 0; i < this.size; i++) {
			try {
				this.semaphore.acquire();
			} catch (Exception ex) {
				System.out.println("Error in MyThreadPool.await(): " + ex.toString());
				System.exit(1);
			}
		}
		for (int i = 0; i < this.size; i++)
			this.semaphore.release();
	}

	public int[] partition(int listSize) {
		int chunk = (listSize - 1) / this.size + 1;
		int[] partition = new int[this.size + 1];
		partition[0] = 0;
		for (int i = 0; i < this.size; i++) {
			int end = (i + 1) * chunk;
			if (end > listSize)
				end = listSize;
			partition[(i + 1)] = end;
		}
		return partition;
	}

	public void execute(Runnable task) {
		try {
			this.semaphore.acquire();
			super.execute(task);
		} catch (Exception ex) {
			System.out.println("Error in MyThreadPool.execute(): " + ex.toString());
			System.exit(1);
		}
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		this.semaphore.release();
	}
}