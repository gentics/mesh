package com.gentics.mesh.graphdb.spi;

import java.io.IOException;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public interface Database {

	FramedThreadedTransactionalGraph getFramedGraph();

	/**
	 * Stop the graph database.
	 */
	void stop();

	/**
	 * Star the graph database.
	 */
	void start();

	/**
	 * Shortcut for stop/start. This will also drop the graph database.
	 */
	void reset();

	/**
	 * Remove all edges and all vertices from the graph.
	 */
	void clear();

	/**
	 * Return a new autoclosable transaction handler. This object should be used within a try-with-resource block.
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(Trx tx = db.trx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	Trx trx();

	/**
	 * Initialize the database and store the settings.
	 * 
	 * @param options
	 */
	void init(StorageOptions options);

	/**
	 * Reload the given mesh element.
	 * 
	 * @param element
	 */
	void reload(MeshElement element);

	/**
	 * Create a database export.
	 * 
	 * @param outputDirectory
	 * @throws IOException
	 */
	void exportGraph(String outputDirectory) throws IOException;

	/**
	 * Import an database export
	 * 
	 * @param importFile
	 * @throws IOException
	 */
	void importGraph(String importFile) throws IOException;

	/**
	 * Create a database backup.
	 * 
	 * @param backupDirectory
	 * @throws IOException
	 */
	void backupGraph(String backupDirectory) throws IOException;

	/**
	 * Restore a previously created database backup.
	 * 
	 * @param backupFile
	 * @throws IOException
	 */
	void restoreGraph(String backupFile) throws IOException;

}
