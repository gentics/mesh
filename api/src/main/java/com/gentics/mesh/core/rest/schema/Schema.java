package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.json.MeshJsonException;

public interface Schema {

	/**
	 * Return the mesh version for this schema.
	 * 
	 * @return
	 */
	public String getMeshVersion();

	/**
	 * Set the mesh version for this schema.
	 * 
	 * @param meshVersion
	 */
	public void setMeshVersion(String meshVersion);

	/**
	 * Return the name of the schema.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Set the schema name.
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Return the display field of the schema.
	 * 
	 * @return
	 */
	public String getDisplayField();

	/**
	 * Set the display field value.
	 * 
	 * @param displayField
	 */
	public void setDisplayField(String displayField);

	/**
	 * Return the binary folder flag.
	 * 
	 * @return
	 */
	public boolean isFolder();

	/**
	 * Set the folder flag for this schema. Nodes that are created using a schema which has an enabled folder flag can be used as a parent for new nodes.
	 * 
	 * @param flag
	 */
	public void setFolder(boolean flag);

	/**
	 * Return the binary flag.
	 * 
	 * @return
	 */
	public boolean isBinary();

	/**
	 * Nodes which are created using a schema that has the binary flag enabled are able to store binary content.
	 * 
	 * @param flag
	 */
	public void setBinary(boolean flag);

	/**
	 * Return the list of field schemas.
	 * 
	 * @return
	 */
	public List<? extends FieldSchema> getFields();

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	public void addField(FieldSchema fieldSchema);

	/**
	 * Return the schema description.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the description of the schema.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	/**
	 * Validate the schema for correctness.
	 * @throws MeshJsonException
	 */
	void validate() throws MeshJsonException;

}
