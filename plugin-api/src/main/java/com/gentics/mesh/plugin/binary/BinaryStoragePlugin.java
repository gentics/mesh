package com.gentics.mesh.plugin.binary;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

/**
 * A binary storage plugin provides means to hook into the upload and download process in order to provide custom storage implementations.
 */
public interface BinaryStoragePlugin extends MeshPlugin {

	/**
	 * Read the binary data which is identified by the given binary uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Flowable<Buffer> read(String uuid);

	/**
	 * Read a segment of the binary.
	 * 
	 * @param uuid
	 * @param start
	 * @param size
	 * @return
	 */
	Flowable<Buffer> read(String uuid, long start, long size);

	/**
	 * Checks whether the binary data for the given binary uuid exists
	 * 
	 * @param uuid
	 * @return
	 */
	Single<Boolean> exists(String uuid);

	/**
	 * Store the binary.
	 * 
	 * @param stream
	 * @param size
	 * @param uuid
	 * @return
	 */
	Completable store(Flowable<Buffer> stream, long size, String uuid);

	/**
	 * Delete the binary with the given uuid.
	 * 
	 * @param uuid
	 */
	Completable delete(String uuid);
}
