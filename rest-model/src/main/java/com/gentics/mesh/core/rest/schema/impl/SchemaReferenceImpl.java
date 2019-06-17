package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * POJO that is used to model a schema reference within a node. Only the name or the uuid of the schema must be supplied when this reference is being used
 * within a node create request / node update request.
 */
public class SchemaReferenceImpl extends AbstractNameUuidReference<SchemaReference> implements SchemaReference {

	private String version;
	private String versionUuid;

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public SchemaReference setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getVersionUuid() {
		return versionUuid;
	}

	@Override
	public SchemaReferenceImpl setVersionUuid(String versionUuid) {
		this.versionUuid = versionUuid;
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "-version:" + version;
	}
}
