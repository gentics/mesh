package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * POJO that is used to model a microschema reference within a node. Only the name or the uuid of the microschema must be supplied when this reference is being
 * used within a node create request / node update request.
 */
public class MicroschemaReference extends NameUuidReference<MicroschemaReference> {

	private String version;

	/**
	 * Return the microschema version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the microschema version.
	 * 
	 * @param version
	 * @return fluent API
	 */
	public MicroschemaReference setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "-version:" + version;
	}
}
