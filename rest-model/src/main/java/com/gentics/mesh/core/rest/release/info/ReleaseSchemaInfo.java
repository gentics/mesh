package com.gentics.mesh.core.rest.release.info;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ReleaseSchemaInfo extends AbstractNameUuidReference<SchemaReference> implements SchemaReference {

	private String version;

	public ReleaseSchemaInfo() {
	}

	public ReleaseSchemaInfo(SchemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
	}

	public String getVersion() {
		return version;
	}

	public ReleaseSchemaInfo setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public ReleaseSchemaInfo setUuid(String uuid) {
		super.setUuid(uuid);
		return this;
	}

	@Override
	public ReleaseSchemaInfo setName(String name) {
		super.setName(name);
		return this;
	}

}
