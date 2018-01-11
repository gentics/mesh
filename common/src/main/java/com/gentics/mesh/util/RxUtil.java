package com.gentics.mesh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Function;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.impl.ReadStreamSubscriber;

public final class RxUtil {

	private RxUtil() {
	}

	public static <T> Completable andThenCompletable(Single<T> source, Function<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.apply(v).toObservable())).ignoreElements();
	}

	public static <T> void noopAction(T nix) {

	}

	/**
	 * Reads the entire stream and returns its contents as a buffer.
	 */
	@Deprecated
	public static Single<Buffer> readEntireData(Observable<Buffer> stream) {
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
	public static InputStream toInputStream(Observable<Buffer> stream, Vertx vertx) throws IOException {
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);
		stream.map(Buffer::getBytes).subscribeOn(RxHelper.blockingScheduler(vertx.getDelegate(), false)).doOnComplete(() -> {
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
		});
		return pis;
	}

	public static Observable<Buffer> toBufferObs(AsyncFile file) {
		return new io.vertx.reactivex.core.file.AsyncFile(file).toObservable()
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate)
			.doOnTerminate(() -> file.close());
	}

	public static io.vertx.reactivex.core.streams.Pump pump1(Observable<Buffer> stream, io.vertx.reactivex.core.file.AsyncFile file) {
		ReadStream<io.vertx.core.buffer.Buffer> rss = ReadStreamSubscriber.asReadStream(stream, Function.identity());
		Pump pump = Pump.pump(rss, file.getDelegate());
		return io.vertx.reactivex.core.streams.Pump.newInstance(pump);
	}

	/**
	 * Creates a pump which applies a workaround for vertx-rxjava#123.
	 * 
	 * @param stream
	 * @param file
	 * @return
	 */
	public static io.vertx.reactivex.core.streams.Pump pump(Observable<io.vertx.reactivex.core.buffer.Buffer> stream,
			io.vertx.reactivex.core.file.AsyncFile file) {
		return pump1(stream.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate), file);

	}
}
