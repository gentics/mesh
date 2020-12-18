package com.gentics.mesh.util.rx;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.WriteStream;

/**
 * Wrapper for write streams which can be used to create an {@link InputStream} via {@link #createInputStream()} from an async source by calling
 * {@link #write(Buffer)} and {@link #end()}.
 */
public class WrapperWriteStream implements WriteStream<Buffer> {

	private static final Logger log = LoggerFactory.getLogger(WrapperWriteStream.class);

	private Buffer buffer;
	private Object bufferLock = new Object();
	private boolean ended = false;
	private int writeQueueMaxSize = 32 * 1024 * 1024; // 32MB
	private PublishSubject<Integer> bufferChanged$;
	private ReplaySubject<Buffer> requested$;

	public WrapperWriteStream() {
		buffer = Buffer.buffer();
		bufferChanged$ = PublishSubject.create();
		requested$ = ReplaySubject.createWithSize(1);
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public synchronized WriteStream<Buffer> write(Buffer data) {
		int length;
		synchronized (bufferLock) {
			buffer.appendBuffer(data);
			length = buffer.length();
		}
		// Writes out alot, thats why logging is disabled
		// log.debug(String.format("Wrote %d bytes", data.length()));
		bufferChanged$.onNext(length);
		return this;
	}

	@Override
	public void end() {
		this.ended = true;
		this.bufferChanged$.onComplete();
		if (endedAndEmpty()) {
			log.debug("End completing");
			requested$.onComplete();
		}
	}

	@Override
	public void end(Buffer buffer) {
		this.write(buffer);
		this.end();
	}

	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		this.writeQueueMaxSize = maxSize;
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return this.buffer.length() >= this.writeQueueMaxSize;
	}

	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		if (this.buffer.length() < writeQueueMaxSize) {
			handler.handle(null);
		} else {
			this.bufferChanged$.filter(len -> len < writeQueueMaxSize).firstOrError().toCompletable().subscribe(() -> handler.handle(null));
		}
		return this;
	}

	/**
	 * Access to the requested buffers.
	 * 
	 * @return
	 */
	public Observable<Buffer> requestedBuffers() {
		return this.requested$;
	}

	/**
	 * Request that the given amount of bytes will be read. This will provide data which can be read via the {@link #createInputStream()} object.
	 * 
	 * @param byteCount
	 */
	public void request(int byteCount) {
		if (byteCount > writeQueueMaxSize) {
			throw new InvalidParameterException("Can't request more than buffer size!");
		}
		Buffer ret;
		boolean ended;
		synchronized (bufferLock) {
			ret = sliceBuffer(byteCount);
			ended = endedAndEmpty();
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("Requested %d bytes", byteCount));
		}

		if (ret != null) {
			if (log.isDebugEnabled()) {
				log.debug("Sent immediately");
			}
			requested$.onNext(ret);
			if (log.isDebugEnabled()) {
				log.debug("Sending complete");
			}
			if (ended) {
				requested$.onComplete();
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Not ready yet");
			}
			Completable enoughBytes$ = this.bufferChanged$.filter(length -> length >= byteCount).firstOrError().toCompletable();
			Completable ended$ = this.bufferChanged$.ignoreElements();

			Completable.ambArray(enoughBytes$, ended$).subscribe(() -> {
				Buffer buf;
				boolean innerended;
				synchronized (bufferLock) {
					buf = sliceBuffer(byteCount);
					innerended = endedAndEmpty();
				}
				requested$.onNext(buf);
				if (innerended) {
					requested$.onComplete();
				}
			});
		}
	}

	private Buffer sliceBuffer(int byteCount) {
		Buffer ret;
		if (buffer == null) {
			return null;
		}

		if (ended && byteCount >= buffer.length()) {
			ret = buffer;
			this.buffer = null;
		} else if (byteCount == this.buffer.length()) {
			ret = buffer;
			buffer = Buffer.buffer();
		} else if (byteCount <= this.buffer.length()) {
			ret = this.buffer.getBuffer(0, byteCount);
			this.buffer = this.buffer.getBuffer(byteCount, buffer.length());
			bufferChanged$.onNext(this.buffer.length());
		} else {
			ret = null;
		}

		return ret;
	}

	/**
	 * Creates a blocking input stream from this write stream. Don't use this with {@link #request(int) request}
	 *
	 * @return
	 */
	public InputStream createInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				log.debug("Read byte");
				if (endedAndEmpty()) {
					return -1;
				}
				int bufLen;
				int ret;
				synchronized (bufferLock) {
					bufLen = buffer.length();
				}
				if (bufLen == 0) {
					bufferChanged$.firstOrError().blockingGet();
				}
				synchronized (bufferLock) {
					Buffer buf = sliceBuffer(1);
					ret = buf.getByte(0);
				}
				return ret;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (endedAndEmpty()) {
					log.debug("read ended");
					return -1;
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("Trying to read %d bytes", len));
				}
				int bufLen, byteCount;
				synchronized (bufferLock) {
					bufLen = buffer.length();
				}

				if (bufLen == 0) {
					if (log.isDebugEnabled()) {
						log.debug("Have to wait for buffer to fill");
					}
					bufferChanged$.firstOrError().blockingGet();
				}
				synchronized (bufferLock) {
					bufLen = buffer.length();
					byteCount = Math.min(len, bufLen);
					sliceBuffer(byteCount).getByteBuf().getBytes(0, b, off, byteCount);
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("Actually read %d bytes", byteCount));
				}
				return byteCount;
			}
		};
	}

	private boolean endedAndEmpty() {
		return ended && (buffer == null || buffer.length() == 0);
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {

	}

}