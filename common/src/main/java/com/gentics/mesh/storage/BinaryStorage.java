package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * A binary storage provides means to store and retrieve binary data.
 */
public interface BinaryStorage {

	/**
	 * Stores the contents of the stream.
	 * 
	 * @param stream
	 * @param uuid
	 *            Uuid of the binary to be stored
	 * @return
	 */
	Completable store(Flowable<Buffer> stream, String uuid);

	/**
	 * Checks whether the binary data for the given field exists
	 * 
	 * @param binaryField
	 * @return
	 */
	boolean exists(BinaryGraphField field);

	/**
	 * Read the binary data which is identified by the given binary uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Flowable<Buffer> read(String uuid);

	/**
	 * Read the entire binary data which is identified by the given binary uuid in the same thread blockingly.
	 *
	 * @param uuid
	 * @return
	 */
	Buffer readAllSync(String uuid);

	/**
	 * Return the local path to the binary if possible. Some storage implementations may only allow stream handling.
	 * 
	 * @param uuid
	 * @return
	 */
	default String getLocalPath(String uuid) {
		return null;
	}

	/**
	 * Delete the binary with the given uuid.
	 * 
	 * @param uuid
	 */
	Completable delete(String uuid);

}
