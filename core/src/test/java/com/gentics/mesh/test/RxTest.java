package com.gentics.mesh.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.gentics.mesh.Mesh;

import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Scheduler;

public class RxTest {

	@Test
	public void testScheduler() throws IOException {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		long start = System.currentTimeMillis();

		List<Observable<String>> list = new ArrayList<>();
		AtomicInteger r = new AtomicInteger();
		for (int i = 0; i < 10; i++) {
			Observable<String> obs = Observable.just("test_" + i);
			obs = obs.observeOn(scheduler);
			obs = obs.map(text -> {
				if (5 == r.incrementAndGet()) {
					throw new RuntimeException("dgsadg");
				}
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Done waiting" + text);
				return text;
			});
			list.add(obs);
		}

		Observable<String> waitFor = Observable.merge(list);
//		waitFor.ob		
		
		
//		Observable<String> sync = Observable.create(sub -> {
//			try {
//				String out = waitFor.toBlocking().last();
//				sub.onNext(out);
//				sub.onCompleted();
//			} catch (Exception e) {
//				System.out.println("Error occured");
//				//				String out = Observable.merge(list).toBlocking().last();
//				//				sub.onNext(out);
//				//				sub.onCompleted();
//				sub.onError(e);
//			}
//		});
		System.out.println("------------");
		waitFor.toBlocking().last();
		long duration = System.currentTimeMillis() - start;
		System.out.println("Execution took: " + duration);
		System.in.read();
	}

	//	@Test
	//	public void testRxError() {
	//
	//		Observable.just("test12").flatMap(bla -> {
	//			return Observable.just("test23").map(bla2 -> {
	//				throw error(BAD_GATEWAY, "adsgsd");
	//			});
	//		}).subscribe(done -> {
	//			System.out.println("test");
	//		} , error -> {
	//			System.out.println("ERROR");
	//			error.printStackTrace();
	//		});
	//	}
}
