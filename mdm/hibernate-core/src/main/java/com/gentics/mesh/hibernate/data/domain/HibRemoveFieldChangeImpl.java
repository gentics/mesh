package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;

import jakarta.persistence.Entity;

/**
 * Implementation of a container change, removing an existing field.
 * 
 * @author plyhun
 *
 */
@Entity(name = "removefieldchangechange")
public class HibRemoveFieldChangeImpl extends AbstractHibSchemaFieldChange implements HibRemoveFieldChange, Serializable {

	private static final long serialVersionUID = 4677044847648166198L;

	public HibRemoveFieldChangeImpl() {
		setOperation(OPERATION);		
	}
}
