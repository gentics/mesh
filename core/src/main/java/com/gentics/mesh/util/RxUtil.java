package com.gentics.mesh.util;

import java.util.List;

import rx.Observable;
import rx.functions.Func2;

public final class RxUtil {

	private RxUtil() {
	}

	/**
	 * Concat the given list of observables and return a single observable that emits the listed elements.
	 * 
	 * @param list
	 * @return
	 */
	public static <T> Observable<T> concatList(List<Observable<T>> list) {
		Observable<T> merged = Observable.empty();
		for (Observable<T> element : list) {
			merged = merged.concatWith(element);
		}
		return merged;
	}

	public static <T> void noopAction(T nix) {

	}

	public final static <T1, T2, R extends Observable<R2>, R2> Observable<R> flatZip(Observable<? extends T1> o1, Observable<? extends T2> o2,
			final Func2<? super T1, ? super T2, Observable<R>> zipFunction) {
		return Observable.zip(o1, o2, zipFunction).flatMap(x -> x);
	}


}
