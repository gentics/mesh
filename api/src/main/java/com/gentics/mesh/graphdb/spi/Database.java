package com.gentics.mesh.graphdb.spi;

import java.io.IOException;
import java.util.function.Consumer;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;

public interface Database {

	/**
	 * Thread local that is used to store references to the used graph.
	 */
	public static ThreadLocal<FramedGraph> threadLocalGraph = new ThreadLocal<>();

	public static void setThreadLocalGraph(FramedGraph graph) {
		Database.threadLocalGraph.set(graph);
	}

	public static FramedGraph getThreadLocalGraph() {
		return Database.threadLocalGraph.get();
	}

	FramedTransactionalGraph startTransaction();

	FramedGraph startNoTransaction();

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

	void trx(Consumer<Trx> tx);

	/**
	 * Return a autoclosable transaction handler. Please note that this method will return a non transaction handler. All actions invoked are executed atomic
	 * and no rollback can be performed. This object should be used within a try-with-resource block.
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(NoTrx tx = db.noTrx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	NoTrx noTrx();

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
