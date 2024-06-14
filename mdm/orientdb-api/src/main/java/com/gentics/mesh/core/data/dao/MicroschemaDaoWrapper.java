package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for {@link HibMicroschema} operations.
 */
public interface MicroschemaDaoWrapper extends PersistingMicroschemaDao {

	/**
	 * Return a list of all microschema container roots to which the microschema container was added.
	 *
	 * @return
	 */
	Result<? extends MicroschemaRoot> getRoots(HibMicroschema schema);

}
