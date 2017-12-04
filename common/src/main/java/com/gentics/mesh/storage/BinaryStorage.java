package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.core.buffer.Buffer;
import rx.Completable;
import rx.Observable;

/**
 * A binary storage provides means to store and retrieve binary data.
 */
public interface BinaryStorage {

	/**
	 * Stores the contents of the stream.
	 * 
	 * @param stream
	 * @param hashsum
	 * @return
	 */
	Completable store(Observable<Buffer> stream, String hashsum);

	/**
	 * Checks whether the binary data for the given field exists
	 * 
	 * @param binaryField
	 * @return
	 */
	boolean exists(BinaryGraphField field);

	/**
	 * Read the binary data which is identified by the given hash sum.
	 * 
	 * @param hashsum
	 * @return
	 */
	Observable<Buffer> read(String hashsum);

}
