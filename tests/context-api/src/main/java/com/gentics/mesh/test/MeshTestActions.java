package com.gentics.mesh.test;

import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;

/**
 * The container for the implementation-dependent test actions.
 * 
 * @author plyhun
 *
 */
public interface MeshTestActions {

	/**
	 * A method for the updating the (micro)schema version entities, which is illegal in the production code.
	 * 
	 * @param <SCV>
	 * @param version
	 * @return
	 */
	public <SCV extends HibFieldSchemaVersionElement<?,?,?,?,?>> SCV updateSchemaVersion(SCV version);
}
