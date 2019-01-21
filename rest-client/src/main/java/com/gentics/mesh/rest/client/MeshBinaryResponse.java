package com.gentics.mesh.rest.client;

import io.reactivex.Flowable;

import java.io.InputStream;
import java.util.Arrays;

public interface MeshBinaryResponse {

	int FLOWABLE_BUFFER_SIZE = 8192;

	/**
	 * Retrieve all bytes of the response body blockingly.
	 * @return
	 */
	byte[] getBytes();

	/**
	 * Retrieve a blocking input stream of the response body.
	 * @return
	 */
	InputStream getStream();

	/**
	 * Retrieve a Flowable which emits byte chunks.
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
