package com.gentics.mesh.madl.index;

import com.gentics.mesh.madl.index.impl.EdgeIndexDefinitionImpl;
import com.gentics.mesh.madl.index.impl.EdgeIndexDefinitionImpl.EdgeIndexDefinitonBuilder;

public interface EdgeIndexDefinition extends ElementIndexDefinition {

	public static EdgeIndexDefinitonBuilder edgeIndex(String label) {
		return new EdgeIndexDefinitionImpl.EdgeIndexDefinitonBuilder(label);
	}

	/**
	 * Return the postfix for the index.
	 * 
	 * @return
	 */
	String getPostfix();

	/**
	 * Whether a dedicate index for in-bound vertices should be created.
	 * 
	 * @return
	 */
	boolean isIncludeIn();

	/**
	 * Whether a dedicate index for in and out bound vertices should be created.
	 * 
	 * @return
	 */
	boolean isIncludeInOut();

	/**
	 * Whether a dedicate index for out-bound vertices should be created.
	 * 
	 * @return
	 */
	boolean isIncludeOut();

}
