package com.syncleus.ferma.type;

import com.syncleus.ferma.index.field.FieldMap;
import com.syncleus.ferma.index.field.FieldType;

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
