package com.gentics.mesh.core.rest.micronode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

/**
 * POJO for the micronode rest response model.
 */
public class MicronodeResponse extends AbstractResponse implements MicronodeField {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the microschema of the micronode.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference microschema;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Fields that are stored in the micronode.")
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

	/**
	 * Shortcut to set the schema reference by microschemaName.
	 * 
	 * @param microschemaName
	 * @return
	 */
	public MicronodeResponse setMicroschemaName(String microschemaName) {
		MicroschemaReference microschemaReference = new MicroschemaReferenceImpl();
		microschemaReference.setName(microschemaName);
		setMicroschema(microschemaReference);
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
