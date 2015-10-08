package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * POJO for a node update request.
 */
public class NodeUpdateRequest implements RestModel {

	private SchemaReference schema;

	private boolean published;

	private String language;

	private FieldMap fields = new FieldMapImpl();

	public NodeUpdateRequest() {
	}

	/**
	 * Return the schema name.
	 * 
	 * @return
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema name.
	 * 
	 * @param schema
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	/**
	 * Return the language of the node.
	 * 
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node which should be updated.
	 * 
	 * @param language
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Set the field which should be updated.
	 * 
	 * @return
	 */
	public FieldMap getFields() {
		return fields;
	}

	/**
	 * Return the published flag.
	 * 
	 * @return
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * Set the published flag.
	 * 
	 * @param published
	 */
	public void setPublished(boolean published) {
		this.published = published;
	}
}
