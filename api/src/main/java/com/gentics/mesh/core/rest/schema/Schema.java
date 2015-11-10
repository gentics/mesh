package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.json.MeshJsonException;

/**
 * Schema which is used for nodes. Various fields can be added to the schema in order build data structures for nodes.
 */
public interface Schema {

	/**
	 * Return the mesh version for this schema.
	 * 
	 * @return Mesh version
	 */
	public String getMeshVersion();

	/**
	 * Set the mesh version for this schema.
	 * 
	 * @param meshVersion
	 *            Mesh version
	 */
	public void setMeshVersion(String meshVersion);

	/**
	 * Return the name of the schema.
	 * 
	 * @return Name of the schema
	 */
	public String getName();

	/**
	 * Set the schema name.
	 * 
	 * @param name
	 *            Name of the schema
	 */
	public void setName(String name);

	/**
	 * Return the display field of the schema which nodes will inherit in order. This is useful when you want to unify the name that should be displayed for
	 * nodes of different types. Nodes that use the folder schema may use the display field name to display the name and blogpost nodes the field title.
	 * 
	 * @return Display field of the schema
	 */
	public String getDisplayField();

	/**
	 * Set the display field value.
	 * 
	 * @param displayField
	 *            Display field
	 */
	public void setDisplayField(String displayField);

	/**
	 * Return the binary folder flag.
	 * 
	 * @return Folder flag value
	 */
	public boolean isFolder();

	/**
	 * Set the folder flag for this schema. Nodes that are created using a schema which has an enabled folder flag can be used as a parent for new nodes.
	 * 
	 * @param flag
	 *            Folder flag value
	 */
	public void setFolder(boolean flag);

	/**
	 * Return the binary flag.
	 * 
	 * @return Binary flag value
	 */
	public boolean isBinary();

	/**
	 * Nodes which are created using a schema that has the binary flag enabled are able to store binary content.
	 * 
	 * @param flag
	 *            Binary flag value
	 */
	public void setBinary(boolean flag);

	/**
	 * Return the list of field schemas.
	 * 
	 * @return List of field schemas
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
	 * @return Schema description
	 */
	String getDescription();

	/**
	 * Set the description of the schema.
	 * 
	 * @param description
	 *            Schema description
	 */
	void setDescription(String description);

	/**
	 * Validate the schema for correctness.
	 * 
	 * @throws MeshJsonException
	 */
	void validate() throws MeshJsonException;

	/**
	 * Removes the field with the given name.
	 * 
	 * @param name
	 */
	public void removeField(String name);

}
