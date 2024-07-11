package com.gentics.mesh.core.search.index.node;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.schema.SchemaModel;

import io.reactivex.Completable;

/**
 * Index handler for node entities.
 */
public interface NodeIndexHandler extends IndexHandler<Node> {

	/**
	 * Validate the schema model and the included ES settings against ES by creating a index template.
	 * 
	 * @param schema
	 * @return
	 */
	Completable validate(SchemaModel schema);

	/**
	 * Return the mapping provider for nodes.
	 */
	NodeContainerMappingProvider getMappingProvider();
}
