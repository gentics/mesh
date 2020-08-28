package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.etc.config.MeshOptions;

public interface TxData extends DaoCollection {

	MeshOptions options();

	HibMeshVersion meshVersion();
}
