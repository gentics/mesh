package com.gentics.mesh.search.index.node;

import com.gentics.mesh.core.rest.schema.SchemaModel;

import io.reactivex.Completable;

public interface NodeIndexHandler {

	Completable validate(SchemaModel schema);

}
