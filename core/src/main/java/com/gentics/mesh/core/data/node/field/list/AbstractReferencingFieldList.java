package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ITEM;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public abstract class AbstractReferencingFieldList<T extends ListableField> extends AbstractFieldList<T> {

	@Override
	public long getSize() {
		return outE(HAS_ITEM).has(getListType()).count();
	}

	protected T addItem(String key, MeshVertex vertex) {
		return addFramedEdge(HAS_ITEM, vertex.getImpl(), getListType());
	}

	@Override
	public List<? extends T> getList() {
		return outE(HAS_ITEM).has(getListType()).toListExplicit(getListType());
	}
}
