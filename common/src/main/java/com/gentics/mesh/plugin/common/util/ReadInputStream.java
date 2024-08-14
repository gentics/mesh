package com.gentics.mesh.plugin.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.streams.ReadStream;

public class ReadInputStream extends InputStream {

    private static final Buffer END_BUFFER = Buffer.buffer(0);
    private static final Buffer EMPTY_BUFFER = Buffer.buffer(0);
    private static final Buffer ERROR_BUFFER = Buffer.buffer(0);

    private static final Logger LOG = LoggerFactory.getLogger(ReadInputStream.class);

    private int availableBytes = 0;

    private long bytesRead = 0;

    /**
     * Flag to indicate that the stream is closed.
     */
    private boolean closed = false;

    private Buffer currentBuffer;

    private IOException exceptionToThrow = null;

    private int pos;

    private final BlockingQueue<Buffer> queue = new LinkedBlockingQueue<>();

    /**
     * Constructs ReadInputStream without any associated handlers
     * configured on a ReadStream.
     */
    public ReadInputStream() {
    }

    public ReadInputStream(final ReadStream<Buffer> readStream) {
        readStream
            .handler(this::populate)
            .endHandler(aVoid -> end());

    }

    @Override
    public int available() throws IOException {
        return availableBytes;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    public void end() {
        queue.add(END_BUFFER);
    }

    /**
     * End the buffer because of an error.
     *
     * @param e
     */
    public void error(final Throwable e) {
        exceptionToThrow = new IOException(e);
        queue.add(ERROR_BUFFER);

    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public void populate(final Buffer buffer) {
        queue.add(buffer);
        availableBytes += buffer.length();
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (currentBuffer == null) {
            try {
                if (queue.isEmpty()) {
                	currentBuffer = EMPTY_BUFFER;
                } else {
                	currentBuffer = queue.take();
                }
                pos = 0;
            } catch (final InterruptedException e) {
                LOG.error("Interrupted while waiting for next buffer", e);
                Thread.currentThread().interrupt();
            }
        }
        if (currentBuffer == null) {
            throw new IOException("Obtained a null buffer from the queue");
        } else if (currentBuffer == ERROR_BUFFER) {
            throw exceptionToThrow;
        } else if (currentBuffer == END_BUFFER) {
            return -1;
        } else if (currentBuffer == EMPTY_BUFFER) {
        	return 0;
        } else {
            // Convert to unsigned byte
            final int b = currentBuffer.getByte(pos++) & 0xFF;
            --availableBytes;
            ++bytesRead;
            if (pos == currentBuffer.length()) {
                currentBuffer = null;
            }
            return b;
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("reset not supported");
    }

    /**
     * Gets a count of how much bytes have been read from this input stream.
     *
     * @return total bytes read
     */
    public long totalBytesRead() {
        return bytesRead;
    }
}
