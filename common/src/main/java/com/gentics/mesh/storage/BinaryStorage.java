package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.util.FileUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.rx.java.RxHelper;
import rx.Completable;
import rx.Observable;

/**
 * A binary storage provides means to store and retrieve binary data.
 */
public interface BinaryStorage {

	/**
	 * Stores the given buffer within the binary storage.
	 * 
	 * @param buffer
	 * @param sha512sum
	 * @deprecated Handling full buffers should be avoided. Use {@link #store(ReadStream, String)} instead.
	 * @return
	 */
	default Completable store(Buffer buffer, String sha512sum) {
		return store(RxHelper.toReadStream(Observable.just(buffer)), sha512sum);
	}

	/**
	 * Stores the contents of the stream.
	 * 
	 * @param stream
	 * @param hashsum
	 * @return
	 */
	Completable store(ReadStream<Buffer> stream, String hashsum);

	/**
	 * Checks whether the binary data for the given field exists
	 * 
	 * @param binaryField
	 * @return
	 */
	boolean exists(BinaryGraphField field);

	/**
	 * Read the binary data which is linked to the field and return the stream.
	 * 
	 * @param field
	 * @return
	 */
	ReadStream<Buffer> read(BinaryGraphField field);

	/**
	 * Hash the given buffer and return a sha512 checksum.
	 * 
	 * @param buffer
	 * @return sha512 checksum
	 */
	static String hashBuffer(Buffer buffer) {
		return FileUtils.generateSha512Sum(buffer);
	}

}
