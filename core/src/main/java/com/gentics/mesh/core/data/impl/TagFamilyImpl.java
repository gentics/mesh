package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;

import java.util.List;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;

public class TagFamilyImpl extends MeshVertexImpl implements TagFamily {

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getDescription() {
		return getProperty("description");
	}

	public void setDescription(String description) {
		setProperty("description", description);
	}

	public List<Tag> getTags() {
		return (List<Tag>) out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	public void addTag(Tag tag) {
		linkOut((TagImpl) tag, HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut((TagImpl) tag, HAS_TAG);
		// TODO delete tag node?!
	}

	public Tag create(String name) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		addTag(tag);
		return tag;
	}

	@Override
	public TagFamilyImpl getImpl() {
		return this;
	}

}
