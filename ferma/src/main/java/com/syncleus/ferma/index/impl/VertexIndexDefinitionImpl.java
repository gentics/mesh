package com.syncleus.ferma.index.impl;

import com.syncleus.ferma.index.IndexDefinition;

public class VertexIndexDefinitionImpl implements IndexDefinition {

	private VertexIndexDefinitionImpl() {
	}

	public static class VertexIndexDefinitonBuilder {

		public VertexIndexDefinitonBuilder() {

		}

		public IndexDefinition build() {
			IndexDefinition def = new VertexIndexDefinitionImpl();

			return def;
		}
	}

}
