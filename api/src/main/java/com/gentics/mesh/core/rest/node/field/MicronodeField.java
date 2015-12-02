package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * Interface for micronode fields
 */
public interface MicronodeField extends ListableField {
	/**
	 * Return the uuid of the micronode.
	 * 
	 * @return Uuid of the micronode
	 */
	String getUuid();

	/**
	 * Get the fields of the micronode
	 * 
	 * @return field of the micronode
	 */
	FieldMap getFields();

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Key of the field to be returned
	 * @param classOfT
	 *            Class of the field
	 * @return Field or null of no field could be found for the given key
	 */
	<T extends Field> T getField(String key, Class<T> classOfT);

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Name of the field
	 * @return Found field or null when no field could be found
	 * @param <T>
	 *            Class of the field
	 */
	<T extends Field> T getField(String key);

	/**
	 * Get the microschema reference used for the micronode
	 * 
	 * @return microschema reference
	 */
	MicroschemaReference getMicroschema();
}
