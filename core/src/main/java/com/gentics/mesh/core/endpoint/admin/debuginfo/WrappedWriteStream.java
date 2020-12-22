package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.io.IOException;
import java.io.OutputStream;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * Wrapper for buffered write streams.
 */
public class WrappedWriteStream extends OutputStream {

	private final WriteStream<Buffer> bufferStream;

	private WrappedWriteStream(WriteStream<Buffer> bufferStream) {
		this.bufferStream = bufferStream;
	}

	/**
	 * Create a new wrapper.
	 * 
	 * @param bufferStream
	 * @return
	 */
	public static WrappedWriteStream fromWriteStream(WriteStream<Buffer> bufferStream) {
		return new WrappedWriteStream(bufferStream);
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		Buffer buffer = Buffer.buffer(len)
			.appendBytes(b, off, len);
		bufferStream.write(buffer);
	}

	@Override
	public void write(byte[] b) throws IOException {
		bufferStream.write(Buffer.buffer(b));
	}
}
