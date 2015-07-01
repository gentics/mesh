package com.gentics.mesh.core.rest.schema;

import java.util.Map;

public interface Schema {

	public String getMeshVersion();

	public void setMeshVersion(String meshVersion);

	public String getSchemaVersion();

	public void setSchemaVersion(String version);

	public String getName();

	public String getDisplayField();

	public boolean isContainer();

	public void setContainer(boolean flag);

	public boolean isBinary();

	public void setBinary(boolean flag);

	public Map<String, ? extends FieldSchema> getFields();

	public void setName(String name);

	public void setDisplayField(String displayField);

	public void addField(String key, FieldSchema fieldSchema);

	String getDescription();

	void setDescription(String description);

}
