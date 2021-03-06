package com.gentics.mesh.madl.index.impl;

import com.gentics.mesh.madl.index.AbstractIndexDefinition;
import com.gentics.mesh.madl.index.AbstractIndexDefinitionBuilder;
import com.gentics.mesh.madl.index.VertexIndexDefinition;

/**
 * @see VertexIndexDefinition
 */
public class VertexIndexDefinitionImpl extends AbstractIndexDefinition implements VertexIndexDefinition {

	private Class<?> clazz;

	private VertexIndexDefinitionImpl() {
	}

	public static class VertexIndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<VertexIndexDefinitionBuilder> {

		private Class<?> clazz;

		public VertexIndexDefinitionBuilder(Class<?> clazz) {
			this.clazz = clazz;
			// By default the index name is the class name of the vertex
			this.name = clazz.getSimpleName();
		}

		/**
		 * Return the builder for a new definition.
		 * 
		 * @return
		 */
		public VertexIndexDefinition build() {
			VertexIndexDefinitionImpl def = new VertexIndexDefinitionImpl();
			def.clazz = clazz;
			def.name = name;
			def.unique = unique;
			def.postfix = postfix;
			def.fields = fields;
			def.type = type;
			return def;
		}

	}

	@Override
	public Class<?> getClazz() {
		return clazz;
	}

}
