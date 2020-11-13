package com.gentics.mesh.search;

import java.util.List;

import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

public interface IndexHandlerRegistry {

	/**
	 * Return a collection which contains all registered handlers.
	 * 
	 * @return
	 */
	List<IndexHandler<?>> getHandlers();

	/**
	 * Return the registered node index handler.
	 * 
	 * @return
	 */
	NodeIndexHandler getNodeIndexHandler();

}
