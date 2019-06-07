package com.syncleus.ferma.type.impl;

import com.syncleus.ferma.type.AbstractTypeDefinition;
import com.syncleus.ferma.type.AbstractTypeDefinitionBuilder;
import com.syncleus.ferma.type.EdgeTypeDefinition;

public class EdgeTypeDefinitionImpl extends AbstractTypeDefinition implements EdgeTypeDefinition {

	private String label;

	public static class EdgeTypeDefinitionBuilder extends AbstractTypeDefinitionBuilder<EdgeTypeDefinitionBuilder> {

		private String label;

		public EdgeTypeDefinitionBuilder(String label) {
			this.label = label;
		}

		public EdgeTypeDefinition build() {
			EdgeTypeDefinitionImpl def = new EdgeTypeDefinitionImpl();
			def.label = label;
			def.superClazz = superClazz;
			return def;
		}

	}

	@Override
	public String getLabel() {
		return label;
	}

}
