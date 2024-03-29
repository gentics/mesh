package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Abstract class for referencing field lists.
 *
 * @param <T>
 *            Type the listed field
 * @param <RM>
 *            Rest model type for the field
 * @param <U>
 *            Type of element that is referenced
 */
public abstract class AbstractReferencingGraphFieldList<T extends HibListableField, RM extends Field, U> extends AbstractGraphFieldList<T, RM, U> implements HibReferencingListField<T, RM, U> {

	@Override
	public int getSize() {
		return (int) outE(HAS_ITEM).has(getListType()).count();
	}

	protected T addItem(String key, MeshVertex vertex) {
		T edge = addFramedEdge(HAS_ITEM, vertex, getListType());
		edge.setFieldKey(key);
		return edge;
	}

	@Override
	public void removeAll() {
		outE(HAS_ITEM).removeAll();
	}

	@Override
	public List<? extends T> getList() {
		return toStream(outE(HAS_ITEM).has(getListType()).frameExplicit(getListType()))
			.sorted((a, b) -> {
				String bk = b.getFieldKey();
				String ak = a.getFieldKey();
				if (bk == null) {
					bk = "0";
				}
				if (ak == null) {
					ak = "0";
				}
				long bv = Long.valueOf(bk);
				long av = Long.valueOf(ak);
				return Long.compare(av, bv);
			}).collect(Collectors.toList());
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		// Detach the list from the given graph field container
		toGraph(container).unlinkOut(this, HAS_LIST);

		// Remove the field if no more containers are attached to it
		if (!in(HAS_LIST).hasNext()) {
			delete(bac);
		}
	}

	@Override
	public GraphField cloneTo(HibFieldContainer container) {
		GraphFieldContainer graphContainer = toGraph(container);
		graphContainer.linkOut(this, HAS_LIST);
		return graphContainer.getList(getClass(), getFieldKey());
	}

	@Override
	public void insertReferenced(int index, U item) {
		addItem(String.valueOf(index + 1), (MeshVertex) item);
	}

	@Override
	public void deleteReferenced(U item) {
		((MeshVertex) item).delete();
	}

	/**
	 * Remove the field by invoking {@link #delete()}.
	 */
	public void removeField() {
		delete();
	}
}
