package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.UserRoot;

public interface TxData {
	UserRoot userDao();

	ProjectRoot projectDao();
}
