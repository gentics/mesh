package com.gentics.mesh.test.performance;

public class BenchmarkJob implements Runnable {

	long duration;

	int complexity;

	public BenchmarkJob(int complexity) {
		this.complexity = complexity;
	}

	public static long fib(int n) {
		if (n <= 1) {
			return n;
		} else {
			return fib(n - 1) + fib(n - 2);
		}
	}

	@Override
	public void run() {
		int n = complexity;
		long start = System.currentTimeMillis();
		for (int i = 1; i <= n; i++) {
			fib(i);
		}
		duration = System.currentTimeMillis() - start;

	}

	public long getDuration() {
		return duration;
	}

}
