package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

public interface ServerSchemaStorage extends SchemaStorage {

	void init();

	/**
	 * Remove the given container from the storage.
	 * 
	 * @param container
	 *            Schema or microschema container which is used to identify the elements which should be removed from the storage
	 */
	void remove(FieldSchemaContainer container);

}
