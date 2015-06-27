package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG_FAMILY;

import java.util.List;

import com.gentics.mesh.core.data.model.TagFamily;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;

public class TagFamilyRootImpl extends MeshVertexImpl implements TagFamilyRoot {

	public TagFamily create(String name) {
		TagFamilyImpl tagFamily = getGraph().addFramedVertex(TagFamilyImpl.class);
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		return tagFamily;
	}

	public List<? extends TagFamilyImpl> getTagFamilies() {
		return out(HAS_TAG_FAMILY).has(TagFamilyImpl.class).toListExplicit(TagFamilyImpl.class);
	}

	private void addTagFamily(TagFamilyImpl tagFamily) {
		linkOut(tagFamily, HAS_TAG_FAMILY);
	}

	public void removeTagFamily(TagFamilyImpl tagFamily) {
		unlinkOut(tagFamily, HAS_TAG_FAMILY);
	}

	@Override
	public TagFamilyRootImpl getImpl() {
		return this;
	}
}
