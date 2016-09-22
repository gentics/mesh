package com.gentics.mesh.test;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.file.FileSystem;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

public class RxTest {

	private Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());

	@Test
	public void testMultipleSingles1() throws InterruptedException {

		List<Single<String>> list = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			list.add(createSingle(i));
		}

		Observable.from(list).flatMap(s -> s.toObservable().subscribeOn(Schedulers.computation())).toList().subscribe(System.out::println,
				Throwable::printStackTrace);
		
		Thread.sleep(10000);
	}

	@Test
	public void testMultipleSingles() {
		List<Single<Tuple<Integer, String>>> list = new ArrayList<>();
		List<String> finalList = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final int current = i;
			finalList.add(null);
			list.add(createSingle(i).map(e -> {
				return Tuple.tuple(current, e);
			}));
		}

		List<Observable<Tuple<Integer, String>>> obsList = list.stream().map(Single::toObservable).collect(Collectors.toList());

		long start = System.currentTimeMillis();
		for (Tuple<Integer, String> tuple : Observable.merge(obsList).toBlocking().toIterable()) {
			finalList.set(tuple.v1(), tuple.v2());
			System.out.println(tuple.v2() + " - " + tuple.v1());
		}

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
		// Iterator<Void> it = obs.toBlocking().getIterator();
		// while (it.hasNext()) {
		// it.next();
		// }

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
