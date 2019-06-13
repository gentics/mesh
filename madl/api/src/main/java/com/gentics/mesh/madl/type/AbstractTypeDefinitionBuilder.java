package com.gentics.mesh.madl.type;

import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;

@SuppressWarnings("unchecked")
public abstract class AbstractTypeDefinitionBuilder<T extends AbstractTypeDefinitionBuilder<T>> {

	protected Class<?> superClazz;

	protected FieldMap fields;

	/**
	 * Add the given fields to the type definition.
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
	 * Set the super clazz of the type.
	 * 
	 * @param clazz
	 * @return Fluent API
	 */
	public T withSuperClazz(Class<?> clazz) {
		this.superClazz = clazz;
		return (T) this;
	}

}
