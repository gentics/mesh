package com.gentics.mesh.graphdb.orientdb;

public class ThreadUtils {

	public static void runAndWait(Runnable runnable) {
		Thread thread = run(runnable);
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done waiting");
	}

	public static Thread run(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.start();
		return thread;
	}

}
