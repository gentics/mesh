package com.gentics.mesh.core.data.db;

import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Definition of data that can be accessed via a Transaction {@link Tx#data()} method.
 */
public interface TxData {

	/**
	 * Mesh server options.
	 * 
	 * @return
	 */
	MeshOptions options();

	/**
	 * Version info.
	 * 
	 * @return
	 */
	HibMeshVersion meshVersion();

	/**
	 * References to permission root elements.
	 * 
	 * @return
	 */
	PermissionRoots permissionRoots();
}
