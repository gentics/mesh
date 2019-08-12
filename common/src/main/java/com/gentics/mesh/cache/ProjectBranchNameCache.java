package com.gentics.mesh.cache;

import java.util.function.Function;

import com.gentics.mesh.core.data.Branch;


/**
 * Cache for project specific branches.
 */
public interface ProjectBranchNameCache extends MeshCache {

	Branch get(String key, Function<String, Branch> mappingFunction);

	EventAwareCache<String, Branch> cache();

}
