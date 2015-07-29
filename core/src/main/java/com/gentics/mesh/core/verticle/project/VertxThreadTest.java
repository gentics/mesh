package com.gentics.mesh.core.verticle.project;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class VertxThreadTest {

	@Test
	public void testThread() throws InterruptedException, IOException {
		Vertx vertx = Vertx.vertx();
		AtomicInteger count = new AtomicInteger();
		Future<Void> future = Future.future();
		future.setHandler(rh1 -> {
			Future<Void> complete = Future.future();
			for (int i = 0; i < 2; i++) {
				System.out.println("Invoking level 1 executor " + i);
				vertx.executeBlocking(bh2 -> {
					System.out.println("CODE l " + count.incrementAndGet());
					if (count.get() == 2) {
						complete.complete();
					}
				}, rh2 -> {
					System.out.println("Result l2 " + count.get());
				});
			}
			CountDownLatch latch1 = new CountDownLatch(1);
			complete.setHandler(ch -> {
				latch1.countDown();
			});
			try {
				latch1.await();
				System.out.println("All completed");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
		future.complete();
		System.in.read();
	}

	@Test
	public void testThread2() throws InterruptedException, IOException {
		Vertx vertx = Vertx.vertx();
		AtomicInteger count = new AtomicInteger();
		for (int i = 0; i < 2; i++) {
			System.out.println("Invoking level 1 executor " + i);
			vertx.executeBlocking(bh -> {
				Future<String> complete = Future.future();
				AtomicInteger count2 = new AtomicInteger();
				for (int r = 0; r < 2; r++) {
					System.out.println("Invoking level 2 executor " + r);
					vertx.executeBlocking(bh2 -> {
						System.out.println("CODE l2 " + count2.incrementAndGet());
						if (count2.get() == 2) {
							complete.complete("Code2");
						}
					}, rh2 -> {
						System.out.println("Result l2 " + count.get());
					});
				}
				complete.setHandler(ch -> {
					System.out.println("REsult: " + complete.result());
					System.out.println("CODE " + count.incrementAndGet());
					
				});
			}, rh -> {
				System.out.println("Result " + count.get());
			});
		}
		System.in.read();
	}
}
