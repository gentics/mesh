package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import com.gentics.mesh.core.data.schema.HibUpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;

import jakarta.persistence.Entity;

/**
 * Implementation of a micronode schema change.
 * 
 * @author plyhun
 *
 */
@Entity(name = "updatemicroschemachange")
public class HibUpdateMicroschemaChangeImpl extends AbstractHibFieldSchemaContainerUpdateChange<MicroschemaModel> implements HibUpdateMicroschemaChange, Serializable {

	private static final long serialVersionUID = 993914144245365464L;

	public HibUpdateMicroschemaChangeImpl() {
		setOperation(OPERATION);		
	}
}
