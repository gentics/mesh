package com.gentics.mesh.core.rest.tag;

import java.util.ArrayList;
import java.util.List;

/**
 * A tag family tag group holds the tag references for a tag family.
 */
public class TagFamilyTagGroup {

	private String uuid;

	private List<TagReference> items = new ArrayList<>();

	public TagFamilyTagGroup() {
	}

	/**
	 * Return the uuid of the tag family.
	 * 
	 * @return Uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the tag family.
	 * 
	 * @param uuid
	 *            Uuid to be set
	 * @return Fluent API
	 */
	public TagFamilyTagGroup setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Return a list of tag references of this tag family group.
	 * 
	 * @return List of tag references
	 */
	public List<TagReference> getItems() {
		return items;
	}
}
