package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * POJO for a node update request.
 */
public class NodeUpdateRequest implements RestModel, FieldContainer {

	private SchemaReference schema;

	private String language;

	private FieldMap fields = new FieldMapImpl();

	public NodeUpdateRequest() {
	}

	/**
	 * Return the schema name.
	 * 
	 * @return Schema reference
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema name.
	 * 
	 * @param schema
	 *            Schema reference
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	/**
	 * Return the language of the node.
	 * 
	 * @return Language tag of the language for the node
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node which should be updated.
	 * 
	 * @param language
	 *            Language tag for the node
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Set the field which should be updated.
	 * 
	 * @return Fields of the node
	 */
	public FieldMap getFields() {
		return fields;
	}
}
