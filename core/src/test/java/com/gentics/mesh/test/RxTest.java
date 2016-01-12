package com.gentics.mesh.test;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;

import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@Ignore("Just used for manual testing")
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
	}

	@Test
	public void testRXFs() {
		Vertx rxVertx = Vertx.newInstance(Mesh.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		fileSystem.existsObservable("/tmp").doOnError(error -> {
			System.out.println("errör");
			throw error(BAD_REQUEST, "node_error_upload_failed", error);
		}).flatMap(e -> {
			System.out.println("blar");
			return Observable.empty();
		}).subscribe();
	}

	@Test
	public void testRxEmpty() {
		Observable<Void> obs = Observable.empty();
		obs = obs.map(e -> {
			System.out.println("obs");
			return null;
		});
		Observable<Void> obs2 = Observable.empty();
		//		Iterator<Void> it = obs.toBlocking().getIterator();
		//		while (it.hasNext()) {
		//			it.next();
		//		}

		Observable<String> obs3 = obs2.flatMap(e -> Observable.just("blub"));

		Observable<Object> merged = Observable.merge(obs, obs3).map(e -> {
			System.out.println("Merged");
			return null;
		});

		merged.toBlocking().last();
		System.out.println("Done");
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
