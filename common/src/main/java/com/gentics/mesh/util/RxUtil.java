package com.gentics.mesh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Function;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Vertx;

public final class RxUtil {

	private static final Logger log = LoggerFactory.getLogger(RxUtil.class);

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

	/**
	 * Provide a blocking {@link InputStream} by reading the byte buffers from the observable.
	 * 
	 * @param stream
	 * @param vertx
	 * @return
	 * @throws IOException
	 */
	// public static InputStream toInputStream(Observable<Buffer> stream, Vertx vertx) throws IOException {
	// WrapperWriteStream wstream = new WrapperWriteStream();
	// stream.observeOn(RxHelper.blockingScheduler(vertx.getDelegate(), false))
	//
	// .doOnComplete(wstream::end)
	//
	// .subscribe(wstream::write);
	// return wstream.createInputStream();
	// }
	public static InputStream toInputStream(Flowable<Buffer> stream, Vertx vertx) throws IOException {
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);
		stream.map(Buffer::getBytes).observeOn(RxHelper.blockingScheduler(vertx.getDelegate(), false)).doOnComplete(() -> {
			try {
				pos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).subscribe(buf -> {
			try {
				pos.write(buf);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, error -> {
			log.error("Error while reading stream", error);
		});
		return pis;
	}

	public static Flowable<Buffer> toBufferFlow(AsyncFile file) {
		return toBufferFlow(new io.vertx.reactivex.core.file.AsyncFile(file));
	}

	public static Flowable<Buffer> toBufferFlow(io.vertx.reactivex.core.file.AsyncFile file) {
		return file.toFlowable()
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate)
			.doOnTerminate(file::close)
			.doOnCancel(file::close);
	}

	public static <T> ObservableTransformer<T, T> onErrorResumeNextIf(Predicate<Throwable> predicate, Observable<T> resumeObservable) {
		return upstream -> upstream.onErrorResumeNext(err ->
			predicate.test(err) ? resumeObservable : Observable.error(err)
		);
	}
}
