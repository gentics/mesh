package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;

public class TagFamily extends MeshVertex {

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

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(Tag.class).toListExplicit(Tag.class);
	}

	public void addTag(Tag tag) {
		linkOut(tag, HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut(tag, HAS_TAG);
		//TODO delete tag node?!
	}

	public Tag create(String name) {
		Tag tag = getGraph().addFramedVertex(Tag.class);
		tag.setName(name);
		addTag(tag);
		return tag;
	}

}
