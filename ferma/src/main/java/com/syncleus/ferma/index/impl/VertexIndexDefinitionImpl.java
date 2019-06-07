package com.syncleus.ferma.index.impl;

import com.syncleus.ferma.index.AbstractIndexDefinition;
import com.syncleus.ferma.index.VertexIndexDefinition;
import com.syncleus.ferma.index.field.FieldMap;

public class VertexIndexDefinitionImpl extends AbstractIndexDefinition implements VertexIndexDefinition {

	private VertexIndexDefinitionImpl() {
	}

	public static class VertexIndexDefinitionBuilder {

		private boolean unique = false;

		private FieldMap fields;

		public VertexIndexDefinitionBuilder() {

		}

		public VertexIndexDefinition build() {
			VertexIndexDefinitionImpl def = new VertexIndexDefinitionImpl();
			def.unique = unique;
			def.fields = fields;
			return def;
		}
	}

}
