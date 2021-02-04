package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.result.Result;

public interface OrientDBSchemaDao extends SchemaDao {

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	Result<? extends SchemaRoot> getRoots(HibSchema schema);
}
