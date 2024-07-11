package com.gentics.mesh.core.endpoint.admin.consistency;

import com.gentics.mesh.core.data.BaseElement;

/**
 * Expected edge definition for the consistency check.
 */
@FunctionalInterface
public interface Edge {

	/**
	 * Follow the given edge label and return the outbound vertex.
	 * 
	 * @param <N>
	 * @param v
	 * @param label
	 * @param clazz
	 * @return
	 */
	<N extends BaseElement> N follow(BaseElement v, String label, Class<N> clazz);
}
