package threadPool;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Taken of the Listing 14.11 of the book Core Java I — Fundamentals 
 * (Tenth Edition) by Cay S. Horstmann
 * 
 * @version 1.0.0 2023-11-06
 * @author joaoGHF
 *
 */
public class ThreadPoolTest {
	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			System.out.print("Enter base directory (e.g. /usr/local/jdk5.0/src): ");
			String directory = in.nextLine();
			
			System.out.print("Enter keyword (e.g. volatile): ");
			String keyword = in.nextLine();
			
			ExecutorService pool = Executors.newCachedThreadPool();
			
			MatchCounter counter = new MatchCounter(new File(directory), keyword, pool);
			
			Future<Integer> result = pool.submit(counter);
			try {
				System.out.println(result.get() + " matching files.");
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			pool.shutdown();
			
			int largestPoolSize = ((ThreadPoolExecutor) pool).getLargestPoolSize();
			System.out.println("largest pool size=" + largestPoolSize);
		}
	}
}

/**
 * This task counts the files in a directory and its subdirectories that
 * contain a given keyword.
 */
class MatchCounter implements Callable<Integer> {
	private File directory;
	private String keyword;
	private ExecutorService pool;
	private int count;
	
	/**
	 * Constructs a MatchCounter
	 * @param directory the directory in which to start the search
	 * @param keyword keyword the keyword to look for
	 * @param pool the thread pool for submitting subtasks
	 */
	public MatchCounter(File directory, String keyword, ExecutorService pool) {
		this.directory = directory;
		this.keyword = keyword;
		this.pool = pool;
	}

	@Override
	public Integer call() {
		count = 0;
		try {
			File[] files = directory.listFiles();
			List<Future<Integer>> results = new ArrayList<>();
			
			for (File file : files) {
				if (file.isDirectory()) {
					MatchCounter counter = new MatchCounter(file, keyword, pool);
					Future<Integer> result = pool.submit(counter);
					results.add(result);
				} else {
					if (search(file)) {
						count++;
					}
				}
			}
			for (Future<Integer> result : results) {
				try {
					count += result.get();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return count;
	}

	/**
	 * Searches s file for a given keyword.
	 * @param file the file to search
	 * @return true if the keyword is contained in the file
	 */
	public boolean search(File file) {
		try {
			try (Scanner in = new Scanner(file, "UTF-8")) {
				boolean found = false;
				while(!found && in.hasNextLine()) {
					String line = in.nextLine();
					if (line.contains(keyword)) {
						found = true;
						System.out.println("'" + keyword + "'' in " + file.getAbsolutePath());
					}
				}
				return found;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
}
