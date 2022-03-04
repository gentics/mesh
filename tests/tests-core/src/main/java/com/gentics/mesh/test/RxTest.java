package com.gentics.mesh.test;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.Errors;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

public class RxTest {

	@Test
	public void testMultipleSingles1() throws InterruptedException {

		List<Single<String>> list = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			list.add(createSingle(i));
		}

		long start = System.currentTimeMillis();
		List<String> finalList = Observable.fromIterable(list).concatMapEager(s -> s.toObservable()).toList().blockingGet();
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
		Vertx rxVertx = Vertx.newInstance(io.vertx.core.Vertx.vertx());
		FileSystem fileSystem = rxVertx.fileSystem();
		fileSystem.rxExists("/tmp").doOnError(error -> {
			System.out.println("errör");
			throw Errors.error(BAD_REQUEST, "node_error_upload_failed", error);
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
}
