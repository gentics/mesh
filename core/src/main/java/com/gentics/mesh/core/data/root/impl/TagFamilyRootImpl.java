package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_FAMILY;

import java.util.List;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

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
