package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;

/**
 * POJO for a tag reference model.
 */
public class TagReference extends AbstractNameUuidReference<TagReference> {

	String tagFamily;

	/**
	 * Return the name of the tag family of the tag.
	 * 
	 * @return
	 */
	public String getTagFamily() {
		return tagFamily;
	}

	/**
	 * Set the tag family of the tag.
	 * 
	 * @param tagFamily
	 * @return Fluent API
	 */
	public TagReference setTagFamily(String tagFamily) {
		this.tagFamily = tagFamily;
		return this;
	}
}
