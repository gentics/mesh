package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag update request model.
 */
public class TagUpdateRequest implements RestModel {

	//TODO check whether reponse also lists this a ...Reference?
	private TagFamilyReference tagFamilyReference;

	// The field is called fields in order to keep it similar to node fields. Maybe one day the tag fields will contain more than just the name. (eg. i18n names)
	private TagFieldContainer fields = new TagFieldContainer();

	public TagUpdateRequest() {
	}

	/**
	 * Return the tag field container.
	 * 
	 * @return
	 */
	public TagFieldContainer getFields() {
		return fields;
	}

	/**
	 * Set the tag field container.
	 * 
	 * @param fields
	 */
	public void setFields(TagFieldContainer fields) {
		this.fields = fields;
	}

	/**
	 * Return the tag family reference.
	 * 
	 * @return
	 */
	public TagFamilyReference getTagFamilyReference() {
		return tagFamilyReference;
	}

	/**
	 * Set the tag family reference.
	 * 
	 * @param tagFamilyReference
	 */
	public void setTagFamilyReference(TagFamilyReference tagFamilyReference) {
		this.tagFamilyReference = tagFamilyReference;
	}

}
