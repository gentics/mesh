package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.AddFieldChange;

import jakarta.persistence.Entity;

/**
 * Implementation of a container change, adding a new field.
 * 
 * @author plyhun
 *
 */
@Entity(name = "addfieldchangechange")
public class HibAddFieldChangeImpl extends AbstractHibSchemaFieldChange implements AddFieldChange, Serializable {

	private static final long serialVersionUID = -8774037600788639829L;

	public HibAddFieldChangeImpl() {
		setOperation(OPERATION);		
	}
}
