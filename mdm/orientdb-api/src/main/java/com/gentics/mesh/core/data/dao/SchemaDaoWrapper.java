package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for schema operation.
 */
public interface SchemaDaoWrapper extends PersistingSchemaDao {

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	Result<? extends SchemaRoot> getRoots(HibSchema schema);

	@Override
	default void onRootDeleted(HibProject root, BulkActionContext bac) {
		HibClassConverter.toGraph(root).getSchemaContainerRoot().delete(bac);
	}
}
