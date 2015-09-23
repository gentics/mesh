package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class NodeReferenceImpl implements NodeReference {

	private String projectName;
	private String uuid;
	private String displayName;
	private SchemaReference schema;;

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

}
