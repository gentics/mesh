package com.gentics.mesh.plugin.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.streams.ReadStream;

public class ReadInputStream extends InputStream {
	private AtomicBoolean finished;
	private AtomicBoolean paused;

	private static final int MAX_QUEUE = 32768;

	private BlockingDeque<Byte> queue;
	private BlockingDeque<Byte> stoppedBuffer;

	private ReadStream<Buffer> inputStream;

	public static ReadInputStream make(ReadStream<Buffer> inputStream) {
		return InnderReadInputStream.make(inputStream);
	}

	private ReadInputStream(ReadStream<Buffer> inputStream) {
		this.paused = new AtomicBoolean(true);
		this.finished = new AtomicBoolean(false);

		this.queue = new LinkedBlockingDeque<Byte>(MAX_QUEUE);
		this.stoppedBuffer = new LinkedBlockingDeque<Byte>();
		this.inputStream = inputStream;

		this.inputStream.handler(handleBuffer -> {
			int index = 0;
			while (index < handleBuffer.length()) {
				if (queue.remainingCapacity() == 0) {
					stoppedBuffer.offerFirst(handleBuffer.getByte(index));
					stop();
				} else {
					queue.offerFirst(handleBuffer.getByte(index));
				}
				index++;

			}
		});
		this.inputStream.endHandler(endHandle -> {
			finished.set(true);
		});
	}

	public ReadInputStream start() {
		this.inputStream.resume();
		this.paused.set(false);
		return this;
	}

	public ReadInputStream stop() {
		this.inputStream.pause();
		this.paused.set(true);
		return this;
	}

	private int readInt() throws IOException, InterruptedException {
		Byte b = null;
		if (finished.get()) {
			if (stoppedBuffer.isEmpty() && queue.isEmpty()) {
				b = null;
			} else {
				if (stoppedBuffer.isEmpty()) {
					b = queue.pollLast(5000, TimeUnit.MILLISECONDS);
				} else {
					b = stoppedBuffer.pollLast();
				}
			}
		} else {
			if (paused.get()) {
				if (queue.isEmpty()) {
					if (stoppedBuffer.isEmpty()) {
						start();
						b = queue.takeLast();
					} else {
						b = stoppedBuffer.pollLast();
					}
				} else {
					b = queue.pollLast(5000, TimeUnit.MILLISECONDS);

				}
			} else {
				if (queue.isEmpty()) {
					if (stoppedBuffer.isEmpty()) {
						start();
						b = queue.takeLast();

					} else {
						b = stoppedBuffer.pollLast();
					}
				} else {
					b = queue.pollLast(5000, TimeUnit.MILLISECONDS);
				}
			}
		}
		return b == null ? -1 : Byte.toUnsignedInt(b);

	}

	@Override
	public int read() throws IOException {

		int result = 0;
		try {
			result = readInt();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		stop();
		super.close();
	}

	@Override
	public int available() throws IOException {
		return 0;
	}

	private static class InnderReadInputStream {
		static ReadInputStream INSTANCE;

		public static ReadInputStream make(ReadStream<Buffer> inputstream) {
			if (INSTANCE == null) {
				INSTANCE = new ReadInputStream(inputstream);
			}
			return INSTANCE;
		}
	}
}
