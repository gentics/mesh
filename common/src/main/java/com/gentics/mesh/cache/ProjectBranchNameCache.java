package com.gentics.mesh.cache;

import com.gentics.mesh.core.data.Branch;

public interface ProjectBranchNameCache {

	EventAwareCache<String, Branch> cache();

}
