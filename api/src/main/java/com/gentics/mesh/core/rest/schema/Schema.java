package com.gentics.mesh.core.rest.schema;

import java.util.List;

public interface Schema {

	public String getMeshVersion();

	public void setMeshVersion(String meshVersion);

	//TODO the user should not version the schema
	public String getSchemaVersion();

	public void setSchemaVersion(String version);

	public String getName();

	public String getDisplayField();

	public boolean isContainer();

	public void setContainer(boolean flag);

	public boolean isBinary();

	public void setBinary(boolean flag);

	public List<? extends FieldSchema> getFields();

	public void setName(String name);

	public void setDisplayField(String displayField);

	public void addField(FieldSchema fieldSchema);

	String getDescription();

	void setDescription(String description);

}
