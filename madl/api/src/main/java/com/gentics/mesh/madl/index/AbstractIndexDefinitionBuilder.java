package com.gentics.mesh.madl.index;

import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;

@SuppressWarnings("unchecked")
public abstract class AbstractIndexDefinitionBuilder<T extends AbstractIndexDefinitionBuilder<T>> {

	protected String postfix;

	protected boolean unique = false;

	protected FieldMap fields;

	protected String name;

	/**
	 * Set the index postfix.
	 * 
	 * @param postfix
	 * @return Fluent API
	 */
	public T withPostfix(String postfix) {
		this.postfix = postfix;
		return (T) this;
	}

	/**
	 * Set the unique flag on the index
	 * 
	 * @return Fluent API
	 */
	public T unique() {
		this.unique = true;
		return (T) this;
	}

	/**
	 * Set the fields for the index.
	 * 
	 * @param fields
	 * @return Fluent API
	 */
	public T withFields(FieldMap fields) {
		if (this.fields == null) {
			this.fields = new FieldMap();
		}

		this.fields.putAll(fields);

		return (T) this;
	}

	/**
	 * Add the given fields to the index definition.
	 * 
	 * @param name
	 * @param type
	 * @return Fluent API
	 */
	public T withField(String name, FieldType type) {
		if (this.fields == null) {
			this.fields = new FieldMap();
		}
		this.fields.put(name, type);

		return (T) this;
	}

	/**
	 * Override the set index name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public T withName(String name) {
		this.name = name;
		return (T) this;
	}

}
