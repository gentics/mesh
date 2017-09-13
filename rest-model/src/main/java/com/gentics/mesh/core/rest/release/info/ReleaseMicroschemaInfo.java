package com.gentics.mesh.core.rest.release.info;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class ReleaseMicroschemaInfo extends AbstractNameUuidReference<MicroschemaReference> implements MicroschemaReference {

	private String version;

	public ReleaseMicroschemaInfo() {
	}

	public ReleaseMicroschemaInfo(MicroschemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
	}

	public String getVersion() {
		return version;
	}

	public ReleaseMicroschemaInfo setVersion(String version) {
		this.version = version;
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
