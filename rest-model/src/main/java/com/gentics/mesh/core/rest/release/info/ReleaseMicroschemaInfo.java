package com.gentics.mesh.core.rest.release.info;

import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class ReleaseMicroschemaInfo extends AbstractReleaseSchemaInfo<MicroschemaReference> implements MicroschemaReference {

	public ReleaseMicroschemaInfo() {
	}

	public ReleaseMicroschemaInfo(MicroschemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
	}

	@Override
	public ReleaseMicroschemaInfo setVersion(String version) {
		super.setVersion(version);
		return this;
	}

	@Override
	public ReleaseMicroschemaInfo setUuid(String uuid) {
		super.setUuid(uuid);
		return this;
	}

	@Override
	public ReleaseMicroschemaInfo setName(String name) {
		super.setName(name);
		return this;
	}

}
