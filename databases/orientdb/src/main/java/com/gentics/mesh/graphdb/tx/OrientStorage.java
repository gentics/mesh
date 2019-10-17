package com.gentics.mesh.graphdb.tx;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.gentics.mesh.graphdb.spi.GraphStorage;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

/**
 * Representation of an orientdb graph storage.
 */
public interface OrientStorage extends GraphStorage {

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
	String backup(String backupDirectory) throws FileNotFoundException, IOException;

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
