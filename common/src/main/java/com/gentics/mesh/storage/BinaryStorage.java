package com.gentics.mesh.storage;

import java.io.InputStream;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.core.buffer.Buffer;

public interface BinaryStorage {

	/**
	 * Stores the given buffer witin the binary storage.
	 * 
	 * @param buffer
	 * @param sha512sum
	 * @param uuid
	 */
	void store(Buffer buffer, String sha512sum, String uuid);

	/**
	 * Checks whether the binary data for the given field exists
	 * 
	 * @param binaryField
	 * @return
	 */
	boolean exists(BinaryGraphField field);

	InputStream read(BinaryGraphField field);

}
