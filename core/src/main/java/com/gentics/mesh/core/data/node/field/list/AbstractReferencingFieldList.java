package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.relationship.MeshRelationships;

public abstract class AbstractReferencingFieldList<T extends ListableField> extends AbstractFieldList<T> {

	@Override
	public List<? extends T> getList() {
		return out(MeshRelationships.HAS_ITEM).has(getListType()).toListExplicit(getListType());
	}
}
