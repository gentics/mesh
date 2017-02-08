package com.gentics.mesh.core.rest.micronode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

	@JsonPropertyDescription("Reference to the microschema of the micronode.")
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
	 * @param microschema
	 *            microschema reference
	 * @return Fluent API
	 */
	public MicronodeResponse setMicroschema(MicroschemaReference microschema) {
		this.microschema = microschema;
		return this;
	}

	@Override
	public FieldMap getFields() {
		return fields;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.MICRONODE.toString();
	}
}
