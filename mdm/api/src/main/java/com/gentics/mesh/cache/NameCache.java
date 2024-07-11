package com.gentics.mesh.cache;

import com.gentics.mesh.core.data.NamedElement;

/**
 * Cache for named elements.
 * 
 * @author plyhun
 *
 * @param <V>
 */
public interface NameCache<V extends NamedElement> extends MeshCache<String, V> {

}
