package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * POJO that is used to model a microschema reference within a node. Only the name or the uuid of the microschema must be supplied when this reference is being
 * used within a node create request / node update request.
 */
public class MicroschemaReferenceImpl extends AbstractNameUuidReference<MicroschemaReference> implements MicroschemaReference {

	private String version;
	private String versionUuid;

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public MicroschemaReference setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getVersionUuid() {
		return versionUuid;
	}

	@Override
	public MicroschemaReferenceImpl setVersionUuid(String versionUuid) {
		this.versionUuid = versionUuid;
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "-version:" + version;
	}
}
