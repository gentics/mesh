package com.gentics.mesh.search.index.node;

import com.gentics.mesh.core.rest.schema.Schema;

import io.reactivex.Completable;

public interface NodeIndexHandler {

	/**
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 * @return
	 */
	Completable validate(Schema schema);

}
