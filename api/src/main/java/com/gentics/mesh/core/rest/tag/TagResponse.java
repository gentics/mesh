package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

/**
 * POJO for a tag response model.
 */
public class TagResponse extends AbstractGenericNodeRestModel {

	private TagFamilyReference tagFamily;

	private TagFieldContainer fields = new TagFieldContainer();

	public TagResponse() {
	}

	/**
	 * Return the tag family reference.
	 * 
	 * @return
	 */
	public TagFamilyReference getTagFamily() {
		return tagFamily;
	}

	/**
	 * Set the tag family reference.
	 * 
	 * @param tagFamily
	 */
	public void setTagFamily(TagFamilyReference tagFamily) {
		this.tagFamily = tagFamily;
	}

	/**
	 * Return the tag field container.
	 * 
	 * @return
	 */
	public TagFieldContainer getFields() {
		return fields;
	}

}
