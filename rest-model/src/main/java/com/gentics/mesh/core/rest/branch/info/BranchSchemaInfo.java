package com.gentics.mesh.core.rest.branch.info;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * POJO for the branch / schemaversion assignment.
 */
public class BranchSchemaInfo extends AbstractBranchSchemaInfo<SchemaReference> implements SchemaReference {

	private String versionUuid;

	public BranchSchemaInfo() {
	}

	public BranchSchemaInfo(SchemaReference reference) {
		this.setUuid(reference.getUuid());
		this.setName(reference.getName());
		this.setVersion(reference.getVersion());
		this.setVersionUuid(reference.getVersionUuid());
	}

	@Setter
	public BranchSchemaInfo setVersion(String version) {
		super.setVersion(version);
		return this;
	}

	@Override
	public BranchSchemaInfo setUuid(String uuid) {
		super.setUuid(uuid);
		return this;
	}

	@Override
	public BranchSchemaInfo setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	public String getVersionUuid() {
		return versionUuid;
	}

	@Override
	public BranchSchemaInfo setVersionUuid(String versionUuid) {
		this.versionUuid = versionUuid;
		return this;
	}

	@Override
	public String toString() {
		return "Name:" + getName() + "@" + getVersion() + "#" + getUuid();
	}

}
