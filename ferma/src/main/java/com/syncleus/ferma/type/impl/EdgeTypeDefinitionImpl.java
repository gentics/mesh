package com.syncleus.ferma.type.impl;

import com.syncleus.ferma.type.EdgeTypeDefinition;

public class EdgeTypeDefinitionImpl implements EdgeTypeDefinition {

	private String label;

	public static class EdgeTypeDefinitionBuilder {

		private String label;

		public EdgeTypeDefinitionBuilder(String label) {
			this.label = label;
		}

		public EdgeTypeDefinition build() {
			EdgeTypeDefinitionImpl def = new EdgeTypeDefinitionImpl();
			def.label = label;
			return def;
		}
	}

	@Override
	public String getLabel() {
		return label;
	}

}
