package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.json.MeshJsonException;

/**
 * Schema which is used for nodes. Various fields can be added to the schema in order build data structures for nodes.
 */
public interface Schema extends FieldSchemaContainer {

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
	 * Return the container flag. When enabled nodes of this schema can contain other nodes. (parent/child relationship)
	 * 
	 * @return Container flag value
	 */
	boolean isContainer();

	/**
	 * Set the container flag for this schema. Nodes that are created using a schema which has an enabled container flag can be used as a parent for new nodes.
	 * 
	 * @param flag
	 *            Container flag value
	 */
	void setContainer(boolean flag);

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
	 * Return the schema version.
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Set the schema version.
	 * 
	 * @param version
	 */
	void setVersion(int version);




}
