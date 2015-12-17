package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.json.MeshJsonException;

/**
 * Schema which is used for nodes. Various fields can be added to the schema in order build data structures for nodes.
 */
public interface Schema {

	/**
	 * Return the name of the schema.
	 * 
	 * @return Name of the schema
	 */
	String getName();

	/**
	 * Set the schema name.
	 * 
	 * @param name
	 *            Name of the schema
	 */
	void setName(String name);

	/**
	 * Return the display field of the schema which nodes will inherit in order. This is useful when you want to unify the name that should be displayed for
	 * nodes of different types. Nodes that use the folder schema may use the display field name to display the name and blogpost nodes the field title.
	 * 
	 * @return Display field of the schema
	 */
	String getDisplayField();

	/**
	 * Set the display field value.
	 * 
	 * @param displayField
	 *            Display field
	 */
	void setDisplayField(String displayField);

	/**
	 * Return the binary folder flag.
	 * 
	 * @return Folder flag value
	 */
	boolean isFolder();

	/**
	 * Set the folder flag for this schema. Nodes that are created using a schema which has an enabled folder flag can be used as a parent for new nodes.
	 * 
	 * @param flag
	 *            Folder flag value
	 */
	void setFolder(boolean flag);

	/**
	 * Return the list of field schemas.
	 * 
	 * @return List of field schemas
	 */
	List<? extends FieldSchema> getFields();

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	void addField(FieldSchema fieldSchema);

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
	void removeField(String name);

	/**
	 * Return the segment field name.
	 * 
	 * @return
	 */
	String getSegmentField();

	/**
	 * Set the segment field name.
	 * 
	 * @param segmentField
	 */
	void setSegmentField(String segmentField);

	/**
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @return
	 */
	FieldSchema getFieldSchema(String fieldName);

}
