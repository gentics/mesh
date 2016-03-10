package com.gentics.mesh.core.rest.micronode;

import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * POJO for the micronode rest response model.
 */
public class MicronodeResponse extends AbstractResponse implements MicronodeField, FieldContainer {

	private MicroschemaReference microschema;

	private FieldMap fields = new FieldMapImpl();

	/**
	 * Get the microschema reference of the micronode
	 * 
	 * @return microschema reference
	 */
	public MicroschemaReference getMicroschema() {
		return microschema;
	}

	/**
	 * Set the microschema reference to the micronode
	 * 
	 * @param microschema microschema reference
	 */
	public void setMicroschema(MicroschemaReference microschema) {
		this.microschema = microschema;
	}

	@Override
	public FieldMap getFields() {
		return fields;
	}

//	/**
//	 * Return the field with the given key.
//	 * 
//	 * @param key
//	 *            Key of the field to be returned
//	 * @param classOfT
//	 *            Class of the field
//	 * @return Field or null of no field could be found for the given key
//	 */
//	public <T extends Field> T getField(String key, Class<T> classOfT) {
//		return getFields().get(key, classOfT);
//	}

//	/**
//	 * Return the field with the given key.
//	 * 
//	 * @param key
//	 *            Name of the field
//	 * @return Found field or null when no field could be found
//	 * @param <T>
//	 *            Class of the field
//	 */
//	@SuppressWarnings("unchecked")
//	public <T extends Field> T getField(String key) {
//		return (T) getFields().get(key);
//	}

	@Override
	public String getType() {
		return FieldTypes.MICRONODE.toString();
	}
}
