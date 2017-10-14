package com.gentics.mesh.util;

import org.junit.Ignore;
import org.junit.Test;

import io.reactivex.Observable;

@Ignore
public class RxUtilTest {

	@Test
	public void testThen() {
		Observable.just(1, 2, 3).doOnNext(item -> {
			System.out.println("Current item " + item.intValue());
		}).lastElement().compose(RxUtil.then(() -> {
			System.out.println("After all");
			return Observable.just(this);
		})).doOnNext(item -> {
			System.out.println(item.getClass());
		}).subscribe();
	}
}