package com.gentics.mesh.cache;

import com.gentics.mesh.core.data.HibNamedElement;

/**
 * Cache for named elements.
 * 
 * @author plyhun
 *
 * @param <V>
 */
public interface NameCache<V extends HibNamedElement> extends MeshCache<String, V> {

}
