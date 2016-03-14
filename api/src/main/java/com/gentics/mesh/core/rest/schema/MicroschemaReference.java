package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * POJO that is used to model a microschema reference within a node. Only the name or the uuid of the microschema must be supplied when this reference is being
 * used within a node create request / node update request.
 */
public class MicroschemaReference extends NameUuidReference<MicroschemaReference> {

	private Integer version;

	/**
	 * Return the microschema version.
	 * 
	 * @return
	 */
	public Integer getVersion() {
		return version;
	}

	/**
	 * Set the microschema version.
	 * 
	 * @param version
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

}
