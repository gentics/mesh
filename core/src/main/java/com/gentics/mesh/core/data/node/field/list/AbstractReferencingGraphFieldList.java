package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractReferencingGraphFieldList<T extends ListableGraphField, RM extends Field, U> extends AbstractGraphFieldList<T, RM, U> {

	@Override
	public long getSize() {
		return outE(HAS_ITEM).has(getListType()).count();
	}

	protected T addItem(String key, MeshVertex vertex) {
		return addFramedEdge(HAS_ITEM, vertex.getImpl(), getListType());
	}

	@Override
	public void removeAll() {
		outE(HAS_ITEM).removeAll();
	}

	@Override
	public List<? extends T> getList() {
		return outE(HAS_ITEM).has(getListType()).toListExplicit(getListType());
	}

	@Override
	public void removeField() {
		delete();
	}
}
