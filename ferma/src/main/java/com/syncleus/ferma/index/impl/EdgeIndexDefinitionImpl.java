package com.syncleus.ferma.index.impl;

import com.syncleus.ferma.index.AbstractIndexDefinition;
import com.syncleus.ferma.index.EdgeIndexDefinition;
import com.syncleus.ferma.index.field.FieldMap;

public class EdgeIndexDefinitionImpl extends AbstractIndexDefinition implements EdgeIndexDefinition {

	private String label;

	private String postfix;

	private boolean includeIn = false;

	private boolean includeOut = false;

	private boolean includeInOut = false;

	private EdgeIndexDefinitionImpl() {
	}

	public static class EdgeIndexDefinitonBuilder {

		private String label;

		private String postfix;

		private boolean unique = false;

		private FieldMap fields;

		private boolean includeIn = false;

		private boolean includeOut = false;

		private boolean includeInOut = false;

		public EdgeIndexDefinitonBuilder(String label) {
			this.label = label;
		}

		public EdgeIndexDefinition build() {
			EdgeIndexDefinitionImpl def = new EdgeIndexDefinitionImpl();
			def.label = label;
			def.postfix = postfix;
			def.unique = unique;
			def.fields = fields;
			def.includeIn = includeIn;
			def.includeOut = includeOut;
			def.includeInOut = includeInOut;
			return def;
		}

		/**
		 * Set the index postfix.
		 * 
		 * @param postfix
		 * @return Fluent API
		 */
		public EdgeIndexDefinitonBuilder withPostfix(String postfix) {
			this.postfix = postfix;
			return this;
		}

		/**
		 * Set the unique flag on the index
		 * 
		 * @return Fluent API
		 */
		public EdgeIndexDefinitonBuilder unique() {
			this.unique = true;
			return this;
		}

		/**
		 * Set the fields for the index.
		 * 
		 * @param fields
		 * @return
		 */
		public EdgeIndexDefinitonBuilder withFields(FieldMap fields) {
			this.fields = fields;
			return this;
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
	public String getLabel() {
		return label;
	}

	@Override
	public String getPostfix() {
		return postfix;
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