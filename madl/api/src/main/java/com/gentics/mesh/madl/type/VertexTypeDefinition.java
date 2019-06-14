package com.gentics.mesh.madl.type;

import com.gentics.mesh.madl.type.impl.VertexTypeDefinitionImpl;
import com.gentics.mesh.madl.type.impl.VertexTypeDefinitionImpl.VertexTypeDefinitionBuilder;

/**
 * Type definition for a vertex.
 */
public interface VertexTypeDefinition extends ElementTypeDefinition {

	/**
	 * Create a new vertex type definition builder for the given vertex class type.
	 * 
	 * @param clazz
	 * @param superClazz
	 *            Super vertex type. If null "V" will be used.
	 */
	public static VertexTypeDefinitionBuilder vertexType(Class<?> clazz, Class<?> superClazz) {
		return new VertexTypeDefinitionImpl.VertexTypeDefinitionBuilder(clazz, superClazz);
	}

	/**
	 * Return the class of the vertex type.
	 * 
	 * @return
	 */
	Class<?> getClazz();

}
