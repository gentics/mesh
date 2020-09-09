package com.gentics.mesh.search.index.node;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.schema.SchemaModel;

import io.reactivex.Completable;

public interface NodeIndexHandler extends IndexHandler<HibNode> {

	Completable validate(SchemaModel schema);

	NodeContainerMappingProvider getMappingProvider();
}
