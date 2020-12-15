package com.gentics.mesh.core.rest.branch.info;

import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * REST POJO for branch microschema information.
 */
public class BranchMicroschemaInfo extends AbstractBranchSchemaInfo<MicroschemaReference> implements MicroschemaReference {

	private String versionUuid;

	public BranchMicroschemaInfo() {
	}

	public BranchMicroschemaInfo(MicroschemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
		this.setVersionUuid(reference.getVersionUuid());
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

	@Override
	public String getVersionUuid() {
		return versionUuid;
	}

	@Override
	public BranchMicroschemaInfo setVersionUuid(String versionUuid) {
		this.versionUuid = versionUuid;
		return this;
	}
}
