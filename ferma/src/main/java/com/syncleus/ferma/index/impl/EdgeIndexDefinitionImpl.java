package com.syncleus.ferma.index.impl;

import com.syncleus.ferma.index.IndexDefinition;

public class EdgeIndexDefinitionImpl implements IndexDefinition {

	private EdgeIndexDefinitionImpl() {
	}

	public static class EdgeIndexDefinitonBuilder {

		public EdgeIndexDefinitonBuilder() {

		}

		public IndexDefinition build() {
			IndexDefinition def = new EdgeIndexDefinitionImpl();

			return def;
		}
	}
}