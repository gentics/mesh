package com.gentics.mesh.madl.index.impl;

import com.gentics.mesh.madl.index.AbstractIndexDefinition;
import com.gentics.mesh.madl.index.AbstractIndexDefinitionBuilder;
import com.gentics.mesh.madl.index.EdgeIndexDefinition;

public class EdgeIndexDefinitionImpl extends AbstractIndexDefinition implements EdgeIndexDefinition {

	private boolean includeIn = false;

	private boolean includeOut = false;

	private boolean includeInOut = false;

	private EdgeIndexDefinitionImpl() {
	}

	public static class EdgeIndexDefinitonBuilder extends AbstractIndexDefinitionBuilder<EdgeIndexDefinitonBuilder> {

		private boolean includeIn = false;

		private boolean includeOut = false;

		private boolean includeInOut = false;

		public EdgeIndexDefinitonBuilder(String label) {
			this.name = label;
		}

		public EdgeIndexDefinition build() {
			EdgeIndexDefinitionImpl def = new EdgeIndexDefinitionImpl();
			def.name = name;
			def.postfix = postfix;
			def.unique = unique;
			def.fields = fields;
			def.includeIn = includeIn;
			def.includeOut = includeOut;
			def.includeInOut = includeInOut;
			return def;
		}

		/**
		 * Whether to include a dedicated index for in-bound vertices.
		 * 
		 * @return Fluent API
		 */
		public EdgeIndexDefinitonBuilder withIn() {
			this.includeIn = true;
			return this;
		}

		/**
		 * Whether to include a dedicated index for out-bound vertices.
		 * 
		 * @return Fluent API
		 */
		public EdgeIndexDefinitonBuilder withOut() {
			this.includeOut = true;
			return this;
		}

		/**
		 * Whether to include a dedicated index for in and out bound vertices.
		 * 
		 * @return Fluent API
		 */
		public EdgeIndexDefinitonBuilder withInOut() {
			this.includeInOut = true;
			return this;
		}
	}

	@Override
	public boolean isIncludeIn() {
		return includeIn;
	}

	@Override
	public boolean isIncludeInOut() {
		return includeInOut;
	}

	@Override
	public boolean isIncludeOut() {
		return includeOut;
	}

}