package com.gentics.mesh.graphdb.orientdb;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Dummy test thread with an internal barrier to simulate blocked threads.
 */
public class TestThread extends Thread {

	private int id;
	private CyclicBarrier barrier;

	public TestThread(int id, CyclicBarrier barrier) {
		this.id = id;
		this.barrier = barrier;
	}

	@Override
	public void run() {
		System.out.println("Waiting in thread " + id);
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Release in thread " + id);

	}

}
