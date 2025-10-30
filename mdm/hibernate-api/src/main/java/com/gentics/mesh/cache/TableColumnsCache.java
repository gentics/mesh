package com.gentics.mesh.cache;

import java.util.Set;

/**
 * A cache for the table column definitions, which could be expensive to obtain.
 */
public interface TableColumnsCache extends MeshCache<Class<?>, Set<String>> {

}
