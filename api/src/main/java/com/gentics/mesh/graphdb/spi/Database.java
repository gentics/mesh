package com.gentics.mesh.graphdb.spi;

import com.gentics.mesh.etc.StorageOptions;
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

}
