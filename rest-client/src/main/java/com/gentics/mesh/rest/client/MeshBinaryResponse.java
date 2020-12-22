package com.gentics.mesh.rest.client;

import io.reactivex.Flowable;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Arrays;

/**
 * The binary response contains additional method to access the binary data via stream or flowable.
 */
public interface MeshBinaryResponse extends Closeable {

	int FLOWABLE_BUFFER_SIZE = 8192;

	/**
	 * Retrieve a blocking input stream of the response body. This object must be closed after the stream has been read.
	 * @return
	 */
	InputStream getStream();

	/**
	 * Retrieve a Flowable which emits byte chunks. The response is closed when all bytes have been read.
	 * It is advised to not use close or the auto closable manually when working with this Flowable, since the bytes
	 * could be emitted asynchronously.
	 * @return
	 */
	default Flowable<byte[]> getFlowable() {
		return Flowable.defer(() -> {
			InputStream stream = getStream();
			return Flowable.generate(emitter -> {
				byte[] buffer = new byte[FLOWABLE_BUFFER_SIZE];
				int count = stream.read(buffer);
				if (count == -1) {
					stream.close();
					close();
					emitter.onComplete();
				} else if (count < FLOWABLE_BUFFER_SIZE) {
					emitter.onNext(Arrays.copyOf(buffer, count));
				} else {
					emitter.onNext(buffer);
				}
			});
		});
	}

	/**
	 * Retrieve the filename of this binary
	 * @return
	 */
	String getFilename();

	/**
	 * Retrieve the content type of the binary
	 * @return
	 */
	String getContentType();

	/**
	 * Closes the response body handler.
	 */
	void close();
}
