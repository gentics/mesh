package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import jakarta.persistence.Entity;

/**
 * Implementation of a container change, changing an existing field's type.
 * 
 * @author plyhun
 *
 */
@Entity(name = "updatefieldchange")
public class HibUpdateFieldChangeImpl extends AbstractHibSchemaFieldChange implements UpdateFieldChange, Serializable {

	private static final long serialVersionUID = -518767977368385155L;

	public HibUpdateFieldChangeImpl() {
		setOperation(OPERATION);		
	}

	@Override
	public void setRestProperty(String key, Object value) {
		// What a restriction removal request comes from the REST API,
		// it gives null instead of an empty array, so the request
		// gets eventually lost. In this case we give the empty array back.
		if (SchemaChangeModel.ALLOW_KEY.equals(key) && value == null) {
			value = new String[0];
		}
		super.setRestProperty(key, value);
	}
}
