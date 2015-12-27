package com.gentics.mesh.test;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.Mesh;

import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class RxTest {

	private Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());

	@Test
	public void testScheduler() throws IOException {
		long start = System.currentTimeMillis();

		Observable<String> waitForA = constructWaitFor();
		Observable<String> waitForB = constructWaitFor();
		waitForA = waitForA.subscribeOn(Schedulers.newThread());
		waitForB = waitForB.subscribeOn(Schedulers.newThread());

		Observable<String> waitFor = Observable.merge(waitForA, waitForB);

		
		System.out.println("------------");
		waitFor.subscribe();
		waitFor.subscribe();
		long duration = System.currentTimeMillis() - start;
		System.out.println("Execution took: " + duration);
		System.in.read();
	}

	private Observable<String> constructWaitFor() {


		return Observable.just("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten").map(text -> {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Done waiting: " + text);
			return text;
		});
	}

	// @Test
	// public void testRxError() {
	//
	// Observable.just("test12").flatMap(bla -> {
	// return Observable.just("test23").map(bla2 -> {
	// throw error(BAD_GATEWAY, "adsgsd");
	// });
	// }).subscribe(done -> {
	// System.out.println("test");
	// } , error -> {
	// System.out.println("ERROR");
	// error.printStackTrace();
	// });
	// }
}
