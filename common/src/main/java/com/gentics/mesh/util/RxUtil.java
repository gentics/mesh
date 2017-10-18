package com.gentics.mesh.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.reactivestreams.Subscription;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.subscribers.DefaultSubscriber;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

public final class RxUtil {

	private RxUtil() {
	}

	public static <T> Completable andThenCompletable(Single<T> source, Function<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.apply(v).toObservable())).ignoreElements();
	}

	public static <T> void noopAction(T nix) {

	}

	public final static <T1, T2, R extends Observable<R2>, R2> Observable<R> flatZip(Observable<? extends T1> o1, Observable<? extends T2> o2,
			final BiFunction<? super T1, ? super T2, Observable<R>> zipFunction) {
		return Observable.zip(o1, o2, zipFunction).flatMap(x -> x);
	}

	/**
	 * Wait for the given observable to complete before emitting any items from the source observable.
	 *
	 * @param o1
	 * @return
	 */
	public static <T> ObservableTransformer<T, T> delay(Observable<?> o1) {
		return source -> {
			return source.delaySubscription(() -> o1.ignoreElements());
		};
	}

	public static <T, U> ObservableTransformer<T, U> then(Callable<Observable<U>> o1) {
		return source -> {
			return Observable.defer(o1).delaySubscription(() -> source.ignoreElements());
		};
	}

	public static <T> Observable<T> concatListNotEager(List<Observable<T>> input) {
		//TODO handle empty list
		return Observable.create(sub -> {
			AtomicInteger index = new AtomicInteger();
			DefaultSubscriber<T> subscriber = new DefaultSubscriber<T>() {
				@Override
				public void onComplete() {
					int current = index.incrementAndGet();
					if (current == input.size()) {
						sub.onComplete();
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

				@Override
				public void onSubscribe(Subscription s) {
					
				}
			};
			input.get(0).subscribe(subscriber);
		});
	}

	/**
	 * Reads the entire AsyncFile object and returns its contents as a buffer.
	 */
	public static Single<Buffer> readEntireFile(AsyncFile file) {
		return new io.vertx.reactivex.core.file.AsyncFile(file).toObservable()
			.reduce((a, b) -> a.appendBuffer(b))
			.toSingle()
			.map(it -> it.getDelegate());
	}
}
