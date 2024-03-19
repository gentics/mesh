package com.gentics.mesh.core.data.job.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobCore;

/**
 * Job implementation for node migrations.
 * 
 * The job class contains code for initialization, handler invocation and finalization of a node migration.
 */
public class NodeMigrationJobImpl extends JobImpl implements JobCore {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeMigrationJobImpl.class, MeshVertexImpl.class);
	}
}
