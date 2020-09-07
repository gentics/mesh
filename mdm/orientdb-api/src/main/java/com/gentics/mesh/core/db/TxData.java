package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.etc.config.MeshOptions;

public interface TxData {

	MeshOptions options();

	HibMeshVersion meshVersion();

	PermissionRoots permissionRoots();
}
