package com.gentics.mesh.plugin.binary;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

/**
 * A {@link BinaryAuxSourcePlugin} provides ways to resolve auxiliary sources that have been defined in the binary sourceId field.
 * 
 * When a source has been defined a download from the source will take precedence over the mesh binary storage.
 */
public interface BinaryAuxSourcePlugin extends MeshPlugin {

	/**
	 * Check whether the plugin can handle auxiliary sources for the given prefix.
	 * 
	 * @param prefix
	 * @return
	 */
	boolean canHandle(String prefix);

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

	// Decide whether update / delete should be supported. Sources implies read-only?
}
