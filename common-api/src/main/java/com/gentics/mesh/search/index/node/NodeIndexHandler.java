package com.gentics.mesh.search.index.node;

import com.gentics.mesh.core.rest.schema.Schema;

import io.reactivex.Completable;

public interface NodeIndexHandler {

	Completable validate(Schema schema);

}
