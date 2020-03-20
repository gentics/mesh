package com.gentics.mesh.search.index.node;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.Schema;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface NodeIndexHandler {

	/**
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 * @return
	 */
	Completable validate(Schema schema);

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return Single with affected index name
	 */
	Single<String> storeContainer(NodeGraphFieldContainer container, String branchUuid, ContainerType type);

}
