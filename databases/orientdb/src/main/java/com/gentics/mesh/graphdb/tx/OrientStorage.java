package com.gentics.mesh.graphdb.tx;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

/**
 * Representation of an orientdb graph storage.
 */
public interface OrientStorage {

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

	/**
	 * Get a raw tinkerpop transaction.
	 * 
	 * @return
	 */
	OrientGraph rawTx();

	/**
	 * Get a raw noTx tinkerpop transaction.
	 * 
	 * @return
	 */
	OrientGraphNoTx rawNoTx();

	/**
	 * Set the mass insertion intent.
	 */
	void setMassInsertIntent();

	/**
	 * Reset the currently set intent to default.
	 */
	void resetIntent();

	/**
	 * Invoke the backup process.
	 * 
	 * @param backupDirectory
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	void backup(String backupDirectory) throws FileNotFoundException, IOException;

	/**
	 * Invoke the restore process.
	 * 
	 * @param backupFile
	 * @throws IOException
	 */
	void restore(String backupFile) throws IOException;

	/**
	 * Export the graph database to the given location.
	 * 
	 * @param outputDirectory
	 * @throws IOException
	 */
	void exportGraph(String outputDirectory) throws IOException;

	/**
	 * Import the graph database to the given location.
	 * 
	 * @param importFile
	 * @throws IOException
	 */
	void importGraph(String importFile) throws IOException;

}
