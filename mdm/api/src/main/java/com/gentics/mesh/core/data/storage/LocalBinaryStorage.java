package com.gentics.mesh.core.data.storage;

/**
 * Storage definition for local filesystem binary data storages.
 */
public interface LocalBinaryStorage extends BinaryStorage {

	/**
	 * Return the local filesystem path for the binary.
	 * 
	 * @param UUID
	 *            of the binary
	 * @return Absolute path
	 */
	String getFilePath(String binaryUuid);

}
