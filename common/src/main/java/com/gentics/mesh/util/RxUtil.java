package com.gentics.mesh.util;

import java.util.function.Function;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class RxUtil {

	private static final Logger log = LoggerFactory.getLogger(RxUtil.class);

	public static final Action NOOP = () -> {};

	private RxUtil() {
	}

	public static <T> Completable andThenCompletable(Single<T> source, Function<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.apply(v).toObservable())).ignoreElements();
	}

	public static <T> void noopAction(T nix) {

	}

	/**
	 * Reads the entire stream and returns its contents as a buffer.
	 * 
	 * @deprecated Try to avoid this method in order to prevent memory issues.
	 */
	@Deprecated
	public static Single<Buffer> readEntireData(Flowable<Buffer> stream) {
		return stream.reduce((a, b) -> a.appendBuffer(b)).toSingle();
	}

	public static Flowable<Buffer> toBufferFlow(AsyncFile file) {
		return toBufferFlow(new io.vertx.reactivex.core.file.AsyncFile(file));
	}

	/**
	 * Transform the async file into a flowable which returns the content. This method will also take care of closing the async file.
	 * 
	 * @param file
	 * @return
	 */
	public static Flowable<Buffer> toBufferFlow(io.vertx.reactivex.core.file.AsyncFile file) {
		return file.toFlowable()
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate)
			.doOnTerminate(file::close)
			.doOnCancel(file::close);
	}

	/**
	 * Flips a completable. Emits an error when the source has completed, and completes when the source emits an error.
	 * 
	 * @param source
	 * @return
	 */
	public static CompletableSource flip(Completable source) {
		return source.toObservable().materialize()
			.map(notificiation -> {
				if (notificiation.isOnError()) {
					return notificiation;
				} else {
					throw new RuntimeException("Completable has succeeded");
				}
			}).ignoreElements();
	}

	/**
	 * Zip the given sources and return single with the result of the zipper.
	 * 
	 * @param source1
	 * @param source2
	 * @param zipper
	 * @return
	 */
	public static <T1, T2, R> Single<R> flatZip(
		SingleSource<? extends T1> source1, SingleSource<? extends T2> source2,
		BiFunction<? super T1, ? super T2, SingleSource<? extends R>> zipper) {
		return Single.zip(source1, source2, zipper).flatMap(x -> x);
	}

	public static <T> Maybe<T> fromNullable(T item) {
		if (item == null) {
			return Maybe.empty();
		} else {
			return Maybe.just(item);
		}
	}
}
