package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class RxUtilTest {

	@Test
	public void testLargeConcat() {
		int amount = 10000;
		Object testObject = new Object();
		ArrayList<Observable<Object>> list = new ArrayList<>(10000);
		for (int i = 0; i < 10000; i++) {
			list.add(Observable.just(testObject));
		}

		int resultCount = RxUtil.concatList(list).count().toBlocking().single();

		assertEquals(amount, resultCount);
	}

	@Test
	public void testConcatWithSlowFirstObservable() {
		AtomicInteger expectedNumber = new AtomicInteger(1);
		AtomicBoolean completed = new AtomicBoolean();
		BehaviorSubject<Integer> sub1 = BehaviorSubject.create();
		BehaviorSubject<Integer> sub2 = BehaviorSubject.create();
		BehaviorSubject<Integer> sub3 = BehaviorSubject.create();

		List<Observable<Integer>> list = Arrays.asList(sub1, sub2, sub3);

		RxUtil.concatList(list).subscribe(number -> {
			assertEquals(expectedNumber.getAndIncrement(), (int) number);
		}, err -> fail("error occurred"), () -> completed.set(true));

		sub2.onNext(4);
		sub2.onNext(5);

		sub3.onNext(7);
		sub3.onNext(8);

		sub2.onNext(6);
		sub3.onNext(9);

		sub2.onCompleted();
		sub3.onCompleted();

		sub1.onNext(1);
		sub1.onNext(2);
		sub1.onNext(3);

		sub1.onCompleted();

		assertTrue("RxUtil.concatList should be completed", completed.get());
		assertEquals("All elements should be emitted", 10, expectedNumber.get());
	}

	@Test
	public void testConcatWithEmptyList() {
		List<Observable<Object>> l = Collections.emptyList();
		int count = RxUtil.concatList(l).count().timeout(2, TimeUnit.SECONDS).toBlocking().single();

		assertEquals("Empty list should return 0 elements", 0, count);
	}

	@Test
	public void testConcatWithSingleObservable() throws Throwable {
		List<Observable<Integer>> l = Collections.singletonList(Observable.just(1, 2, 3));

		int count = testCountingObservable(RxUtil.concatList(l));

		assertEquals("It should emit 3 numbers", 3, count);
	}

	@Test(expected = NullPointerException.class)
	public void testConcatWithListWithNulls() {
		List<Observable<Integer>> l = Arrays.asList(Observable.just(1, 2, 3), null, Observable.just(4, 5, 6));

		RxUtil.concatList(l).toBlocking().last();
	}

	@Test(expected = TestException.class)
	public void testConcatErrorBeforeSub() throws Throwable {
		List<Observable<Integer>> l = Arrays.asList(Observable.error(new TestException()), Observable.just(1, 2, 3), Observable.just(4, 5, 6));

		int count = testCountingObservable(RxUtil.concatList(l));
		assertEquals(6, count);
	}

	private class TestException extends Exception {

	}

	/**
	 * Checks if the given observable counts from 1 in the correct order.
	 *
	 * @param counting
	 *            The observable to check for
	 * @return How many numbers have been emitted.
	 */
	private int testCountingObservable(Observable<Integer> counting) throws Throwable {
		AtomicInteger counter = new AtomicInteger(1);
		try {
			counting.toBlocking().forEach(i -> {
				assertEquals(counter.getAndIncrement(), (int) i);
			});
		} catch (RuntimeException e) {
			if (e.getCause() != null) {
				throw e.getCause();
			} else {
				throw e;
			}
		}
		return counter.get() - 1;
	}

	@Test
	public void testThen() {
		Observable.just(1, 2, 3).doOnNext(item -> {
			System.out.println("Current item " + item.intValue());
		}).last().compose(RxUtil.then(() -> {
			System.out.println("After all");
			return Observable.just(this);
		})).doOnNext(item -> {
			System.out.println(item.getClass());
		}).subscribe();
	}
}