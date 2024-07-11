package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.FieldTypeChange;

import jakarta.persistence.Entity;

/**
 * Implementation of a container change, changing a field's type.
 * 
 * @author plyhun
 *
 */
@Entity(name = "fieldtypechange")
public class HibFieldTypeChangeImpl extends AbstractHibSchemaFieldChange implements FieldTypeChange, Serializable {

	private static final long serialVersionUID = -8208474420713192825L;

	public HibFieldTypeChangeImpl() {
		setOperation(OPERATION);		
	}
}
