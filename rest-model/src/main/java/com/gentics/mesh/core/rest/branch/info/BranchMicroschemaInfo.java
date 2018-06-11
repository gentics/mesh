package com.gentics.mesh.core.rest.branch.info;

import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class BranchMicroschemaInfo extends AbstractBranchSchemaInfo<MicroschemaReference> implements MicroschemaReference {

	public BranchMicroschemaInfo() {
	}

	public BranchMicroschemaInfo(MicroschemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
	}

	@Override
	public BranchMicroschemaInfo setVersion(String version) {
		super.setVersion(version);
		return this;
	}

	@Override
	public BranchMicroschemaInfo setUuid(String uuid) {
		super.setUuid(uuid);
		return this;
	}

	@Override
	public BranchMicroschemaInfo setName(String name) {
		super.setName(name);
		return this;
	}

}
