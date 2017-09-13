package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class ReleaseSchemaInfo extends AbstractNameUuidReference<SchemaReference> implements SchemaReference {

	private String version;

	public ReleaseSchemaInfo() {
	}

	public ReleaseSchemaInfo(SchemaReferenceImpl reference) {
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
