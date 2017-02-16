package com.gentics.mesh.test;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.util.RxUtil;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class RxTest {

	@Test
	public void testMultipleSingles1() throws InterruptedException {

		List<Single<String>> list = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			list.add(createSingle(i));
		}

		long start = System.currentTimeMillis();
		List<String> finalList = Observable.from(list).concatMapEager(s -> s.toObservable()).toList().toSingle().toBlocking().value();
		for (String value : finalList) {
			System.out.println(value);
		}
		long duration = System.currentTimeMillis() - start;
		System.out.println("Duration: " + duration);
	}

	private Single<String> createSingle(int i) {
		return Single.create(sub -> {
			new Thread(() -> {
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sub.onSuccess("test_" + i);
			}).start();
		});
	}

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
		fileSystem.rxExists("/tmp").doOnError(error -> {
			System.out.println("errör");
			throw error(BAD_REQUEST, "node_error_upload_failed", error);
		}).flatMap(e -> {
			System.out.println("blar");
			return Single.just(false);
		}).subscribe();
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

	@Test
	public void testFlatMap() throws Exception {

		Observable<String> obs = Observable.just("hallo").flatMap(item -> {
			System.out.println("FLATMAP");
			return Observable.create(sub -> {
				sub.onNext("Tüte");
				sub.onCompleted();
			});
		});

		List<Observable<String>> obsAll = new ArrayList<>();
		obsAll.add(obs);

		RxUtil.concatListNotEager(obsAll).subscribe(next -> {
			System.out.println(next);
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
