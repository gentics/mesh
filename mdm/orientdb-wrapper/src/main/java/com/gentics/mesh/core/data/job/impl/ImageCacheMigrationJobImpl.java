package com.gentics.mesh.core.data.job.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobCore;

/**
 * Implementation of image cache migration job
 */
public class ImageCacheMigrationJobImpl extends JobImpl implements JobCore {
	/**
	 * Initialize the graphdb
	 * @param type type handler
	 * @param index index handler
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ImageCacheMigrationJobImpl.class, MeshVertexImpl.class);
	}
}
