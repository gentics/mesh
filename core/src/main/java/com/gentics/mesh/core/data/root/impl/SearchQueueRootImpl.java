package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ITEM;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.SearchQueueRoot;

public class SearchQueueRootImpl extends MeshVertexImpl implements SearchQueueRoot {

	@Override
	public void removeElement(GenericVertex<?> element) {
		unlinkOut(element.getImpl(), HAS_ITEM);
	}

	@Override
	public void addElement(GenericVertex<?> element) {
		linkOut(element.getImpl(), HAS_ITEM);
	}

	@Override
	public GenericVertex<?> getNext() {
		return out(HAS_ITEM).nextOrDefault(GenericVertex.class, null);
	}

}
