package com.gentics.mesh.graphdb.spi;

public interface GraphStorage {

	String DB_NAME = "storage";

	/**
	 * Open the storage with the given name.
	 * 
	 * @param name
	 */
	void open(String name);

	/**
	 * Open the default storage.
	 */
	default void open() {
		open(DB_NAME);
	}

	/**
	 * Close the currently opened storage.
	 */
	void close();

	/**
	 * Clear the data within the storage.
	 */
	void clear();
}
