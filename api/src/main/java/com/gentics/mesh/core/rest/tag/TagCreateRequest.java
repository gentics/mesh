package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

public class TagCreateRequest implements RestModel {

	private TagFieldContainer fields = new TagFieldContainer();

	public TagCreateRequest() {
	}

	/**
	 * Return the tag field container which holds tag values (e.g.: Tag name)
	 * 
	 * @return Tag field container
	 */
	public TagFieldContainer getFields() {
		return fields;
	}

	/**
	 * Set the field container which hold the tag values (e.g.: Tag name)
	 * 
	 * @param fields
	 * @return Fluent API
	 */
	public TagCreateRequest setFields(TagFieldContainer fields) {
		this.fields = fields;
		return this;
	}

}
