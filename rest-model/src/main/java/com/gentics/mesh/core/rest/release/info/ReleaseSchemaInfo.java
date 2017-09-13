package com.gentics.mesh.core.rest.release.info;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ReleaseSchemaInfo extends AbstractReleaseSchemaInfo<SchemaReference> implements SchemaReference {

	public ReleaseSchemaInfo() {
	}

	public ReleaseSchemaInfo(SchemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
	}

	public ReleaseSchemaInfo setVersion(String version) {
		super.setVersion(version);
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
