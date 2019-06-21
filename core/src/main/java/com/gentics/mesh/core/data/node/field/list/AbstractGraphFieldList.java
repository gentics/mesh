package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Abstract class for field lists.
 * 
 * @param <T>
 * @param <RM>
 *            Rest model type of the list
 * @param <U>
 *            Type of the listed element
 */
public abstract class AbstractGraphFieldList<T extends ListableGraphField, RM extends Field, U> extends MeshVertexImpl
		implements ListGraphField<T, RM, U> {

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void addItem(T item) {
		// TODO Auto-generated method stub
	}

	@Override
	public void removeItem(T item) {
		// TODO Auto-generated method stub
	}

	@Override
	public void validate() {
		getList().stream().forEach(GraphField::validate);
	}

	@Override
	public GraphFieldContainer getGraphFieldContainer() {
		return inE(HAS_LIST).next(GraphFieldContainer.class);
	}
}
