package com.gentics.mesh.core.data.job.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.MicronodeMigrationJob;

/**
 * Implementation of the micronode migration job.
 */
public class MicronodeMigrationJobImpl extends JobImpl implements MicronodeMigrationJob {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}
}
