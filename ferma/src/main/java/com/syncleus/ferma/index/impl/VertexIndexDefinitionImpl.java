package com.syncleus.ferma.index.impl;

import com.syncleus.ferma.index.AbstractIndexDefinition;
import com.syncleus.ferma.index.AbstractIndexDefinitionBuilder;
import com.syncleus.ferma.index.VertexIndexDefinition;

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

		public VertexIndexDefinition build() {
			VertexIndexDefinitionImpl def = new VertexIndexDefinitionImpl();
			def.clazz = clazz;
			def.name = name;
			def.unique = unique;
			def.postfix = postfix;
			def.fields = fields;
			return def;
		}

	}

	@Override
	public Class<?> getClazz() {
		return clazz;
	}

}
