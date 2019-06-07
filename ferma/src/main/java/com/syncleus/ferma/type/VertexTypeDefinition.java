package com.syncleus.ferma.type;

import com.syncleus.ferma.type.impl.VertexTypeDefinitionImpl;
import com.syncleus.ferma.type.impl.VertexTypeDefinitionImpl.VertexTypeDefinitionBuilder;

public interface VertexTypeDefinition extends ElementTypeDefinition {

	/**
	 * Create a new vertex type definition builder for the given vertex class type.
	 * 
	 * @param clazzOfVertex
	 * @param superClazzOfVertex
	 *            Super vertex type. If null "V" will be used.
	 */
	public static VertexTypeDefinitionBuilder vertexType(Class<?> clazzOfVertex, Class<?> superClazzOfVertex) {
		return new VertexTypeDefinitionImpl.VertexTypeDefinitionBuilder(clazzOfVertex, superClazzOfVertex);
	}

	/**
	 * Return the class of the vertex type.
	 * 
	 * @return
	 */
	Class<?> getClazz();

	/***
	 * Return the super class of the vertex type.
	 * 
	 * @return
	 */
	Class<?> getSuperClazz();

}
