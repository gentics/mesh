package com.gentics.mesh.test;

import org.junit.Test;

import io.reactivex.Observable;

public class RxDebuggerTest {

	@Test
	public void testStuckObservable() throws InterruptedException {
		new Thread(() -> {
			Observable<String> obs = Observable.create(sub -> {
				for (int i = 0; i < 10; i++) {
					sub.onNext("blub");
				}
				try {
					Thread.sleep(4000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Complete");
				sub.onComplete();

			});
			obs.subscribe();
		}).start();
		Thread.sleep(10000);
		System.out.println("Done waiting");
	}

	@Test
	public void testStuckObservableWithNoEmit() throws InterruptedException {
		new Thread(() -> {
			Observable<String> obs = Observable.create(sub -> {
				try {
					Thread.sleep(4000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Complete");
				sub.onComplete();

			});
			obs.subscribe();
		}).start();
		Thread.sleep(10000);
		System.out.println("Done waiting");
	}

}
