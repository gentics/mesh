package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.GraphField;
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
	public void removeField(GraphFieldContainer container) {
		container.getImpl().unlinkOut(getImpl(), HAS_LIST);

		if (in(HAS_LIST).count() == 0) {
			delete(null);
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		container.getImpl().linkOut(getImpl(), HAS_LIST);
		return container.getList(getClass(), getFieldKey());
	}

	public void removeField() {
		delete(null);
	}
}
