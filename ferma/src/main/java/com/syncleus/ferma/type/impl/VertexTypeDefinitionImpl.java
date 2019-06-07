package com.syncleus.ferma.type.impl;

import com.syncleus.ferma.type.VertexTypeDefinition;

public class VertexTypeDefinitionImpl implements VertexTypeDefinition {

	private Class<?> clazz;
	private Class<?> superClazz;

	public static class VertexTypeDefinitionBuilder {

		private Class<?> clazzOfVertex;
		private Class<?> superClazzOfVertex;

		/**
		 * Create a new vertex type definition builder for the given vertex class type.
		 * 
		 * @param clazzOfVertex
		 * @param superClazzOfVertex
		 *            Super vertex type. If null "V" will be used.
		 */
		public VertexTypeDefinitionBuilder(Class<?> clazzOfVertex, Class<?> superClazzOfVertex) {
			this.clazzOfVertex = clazzOfVertex;
			this.superClazzOfVertex = superClazzOfVertex;
		}

		public VertexTypeDefinition build() {
			VertexTypeDefinitionImpl def = new VertexTypeDefinitionImpl();
			def.clazz = clazzOfVertex;
			def.superClazz = superClazzOfVertex;
			return def;
		}
	}

	@Override
	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public Class<?> getSuperClazz() {
		return superClazz;
	}
}
