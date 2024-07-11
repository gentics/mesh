package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.HibUpdateSchemaChange;
import com.gentics.mesh.core.rest.schema.SchemaModel;

import jakarta.persistence.Entity;

/**
 * Implementation of a container type change.
 * 
 * @author plyhun
 *
 */
@Entity(name = "updateschemachange")
public class HibUpdateSchemaChangeImpl extends AbstractHibFieldSchemaContainerUpdateChange<SchemaModel> implements HibUpdateSchemaChange, Serializable {

	private static final long serialVersionUID = 3852962439530321185L;

	public HibUpdateSchemaChangeImpl() {
		setOperation(OPERATION);		
	}

}
