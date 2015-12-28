package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag update request model.
 */
public class TagUpdateRequest implements RestModel {

	//private TagFamilyReference tagFamily;

	// The field is called fields in order to keep it similar to node fields. Maybe one day the tag fields will contain more than just the name. (eg. i18n names)
	private TagFieldContainer fields = new TagFieldContainer();

	public TagUpdateRequest() {
	}

	/**
	 * Return the tag field container which holds tag values (eg. Tag name)
	 * 
	 * @return Tag field container
	 */
	public TagFieldContainer getFields() {
		return fields;
	}

	/**
	 * Set the tag field container.
	 * 
	 * @param fields
	 *            Tag field container
	 */
	public void setFields(TagFieldContainer fields) {
		this.fields = fields;
	}

//	/**
//	 * Return the tag family reference.
//	 * 
//	 * @return Tag family reference
//	 */
//	public TagFamilyReference getTagFamily() {
//		return tagFamily;
//	}
//
//	/**
//	 * Set the tag family reference.
//	 * 
//	 * @param tagFamily
//	 *            Tag family reference
//	 */
//	public void setTagFamily(TagFamilyReference tagFamily) {
//		this.tagFamily = tagFamily;
//	}

}
