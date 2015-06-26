package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG_FAMILY;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class TagFamilyRoot extends MeshVertex {

	public TagFamily create(String name) {
		TagFamily tagFamily = getGraph().addFramedVertex(TagFamily.class);
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		return tagFamily;
	}

	public List<? extends TagFamily> getTagFamilies() {
		return out(HAS_TAG_FAMILY).has(TagFamily.class).toListExplicit(TagFamily.class);
	}

	private void addTagFamily(TagFamily tagFamily) {
		linkOut(tagFamily, HAS_TAG_FAMILY);
	}

	public void removeTagFamily(TagFamily tagFamily) {
		unlinkOut(tagFamily, HAS_TAG_FAMILY);
	}

}
