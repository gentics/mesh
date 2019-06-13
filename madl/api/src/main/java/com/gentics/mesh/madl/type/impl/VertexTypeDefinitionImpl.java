package com.gentics.mesh.madl.type.impl;

import com.gentics.mesh.madl.type.AbstractTypeDefinition;
import com.gentics.mesh.madl.type.AbstractTypeDefinitionBuilder;
import com.gentics.mesh.madl.type.VertexTypeDefinition;

public class VertexTypeDefinitionImpl extends AbstractTypeDefinition implements VertexTypeDefinition {

	private Class<?> clazz;

	public static class VertexTypeDefinitionBuilder extends AbstractTypeDefinitionBuilder<VertexTypeDefinitionBuilder> {

		private Class<?> clazz;

		/**
		 * Create a new vertex type definition builder for the given vertex class type.
		 * 
		 * @param clazz
		 * @param superClazz
		 *            Super vertex type. If null "V" will be used.
		 */
		public VertexTypeDefinitionBuilder(Class<?> clazz, Class<?> superClazz) {
			this.clazz = clazz;
			this.superClazz = superClazz;
		}

		public VertexTypeDefinition build() {
			VertexTypeDefinitionImpl def = new VertexTypeDefinitionImpl();
			def.clazz = clazz;
			def.fields = fields;
			def.superClazz = superClazz;
			return def;
		}
	}

	@Override
	public Class<?> getClazz() {
		return clazz;
	}

}