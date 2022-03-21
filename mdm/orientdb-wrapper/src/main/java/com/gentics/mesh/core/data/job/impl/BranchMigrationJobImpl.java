package com.gentics.mesh.core.data.job.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobCore;

/**
 * Job implementation to be used for persisting and invoking branch migrations.
 */
public class BranchMigrationJobImpl extends JobImpl implements JobCore {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BranchMigrationJobImpl.class, MeshVertexImpl.class);
	}
}
