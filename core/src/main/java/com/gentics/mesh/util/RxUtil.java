package com.gentics.mesh.util;

import java.util.List;

import rx.Observable;

public final class RxUtil {

	private RxUtil() {
	}

	/**
	 * Concat the given list of observables and return a single observable that emits the listed elements.
	 * 
	 * @param list
	 * @return
	 */
	public static <T> Observable<T> concatList(List<? extends Observable<T>> list) {
		Observable<T> merged = Observable.empty();
		for (Observable<T> element : list) {
			merged = merged.concatWith(element);
		}
		return merged;
	}
}
