package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for schema operation.
 */
public interface SchemaDaoWrapper extends SchemaDao, OrientDBDaoGlobal<HibSchema> {

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	Result<? extends SchemaRoot> getRoots(HibSchema schema);

}
