package com.gentics.mesh.madl.type.impl;

import com.gentics.mesh.madl.type.AbstractTypeDefinition;
import com.gentics.mesh.madl.type.AbstractTypeDefinitionBuilder;
import com.gentics.mesh.madl.type.EdgeTypeDefinition;

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
