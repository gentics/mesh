package com.gentics.mesh.util;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

import java.util.function.Function;

public final class RxUtil {

	private RxUtil() {
	}

	public static <T> Completable andThenCompletable(Single<T> source, Function<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.apply(v).toObservable())).ignoreElements();
	}

	public static <T> void noopAction(T nix) {

	}

	public final static <T1, T2, R> Observable<R> flatZip(ObservableSource<? extends T1> o1, ObservableSource<? extends T2> o2,
																					 final BiFunction<? super T1, ? super T2, ? extends Observable<R>> zipFunction) {
		return Observable.zip(o1, o2, zipFunction).flatMap(x -> x);
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
