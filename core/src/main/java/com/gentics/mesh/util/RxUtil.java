package com.gentics.mesh.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public final class RxUtil {

	private RxUtil() {
	}

	public static <T> Completable andThenCompletable(Single<T> source, Func1<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.call(v).toObservable())).toCompletable();
	}

	public static <T> void noopAction(T nix) {

	}

	public final static <T1, T2, R extends Observable<R2>, R2> Observable<R> flatZip(Observable<? extends T1> o1, Observable<? extends T2> o2,
			final Func2<? super T1, ? super T2, Observable<R>> zipFunction) {
		return Observable.zip(o1, o2, zipFunction).flatMap(x -> x);
	}

	/**
	 * Wait for the given observable to complete before emitting any items from the source observable.
	 *
	 * @param o1
	 * @return
	 */
	public static <T> Transformer<T, T> delay(Observable<?> o1) {
		return source -> {
			return source.delaySubscription(() -> o1.ignoreElements());
		};
	}

	public static <T, U> Transformer<T, U> then(Func0<Observable<U>> o1) {
		return source -> {
			return Observable.defer(o1).delaySubscription(() -> source.ignoreElements());
		};
	}

	public static <T> Observable<T> concatListNotEager(List<Observable<T>> input) {
		//TODO handle empty list
		return Observable.create(sub -> {
			AtomicInteger index = new AtomicInteger();
			Subscriber<T> subscriber = new Subscriber<T>() {
				@Override
				public void onCompleted() {
					int current = index.incrementAndGet();
					if (current == input.size()) {
						sub.onCompleted();
					} else {
						input.get(current).subscribe(this);
					}
				}

				@Override
				public void onError(Throwable e) {
					sub.onError(e);
				}

				@Override
				public void onNext(T o) {
					sub.onNext(o);
				}
			};
			input.get(0).subscribe(subscriber);
		});
	}

}
