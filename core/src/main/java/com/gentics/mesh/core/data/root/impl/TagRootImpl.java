package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;

public class TagRootImpl extends AbstractRootVertex<Tag>implements TagRoot {

	@Override
	protected Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(Tag tag) {
		addItem(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		removeItem(tag);
	}

	@Override
	public Tag findByName(String name) {
		return out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back()
				.nextOrDefaultExplicit(TagImpl.class, null);
	}
	
	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}

}
