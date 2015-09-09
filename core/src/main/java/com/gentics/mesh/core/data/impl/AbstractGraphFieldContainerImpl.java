package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.SelectGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.MicroschemaGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicroschemaGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public abstract class AbstractGraphFieldContainerImpl extends AbstractBasicGraphFieldContainerImpl implements GraphFieldContainer {

	public List<String> getFieldnames() {
		return null;
	}

	@Override
	public StringGraphField createString(String key) {
		// TODO check whether the key is already occupied
		StringGraphFieldImpl field = new StringGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public StringGraphField getString(String key) {
		if (fieldExists(key)) {
			return new StringGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NodeGraphField createNode(String key, Node node) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NodeGraphField getNode(String key) {
		return outE(HAS_FIELD).has(NodeGraphFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(NodeGraphFieldImpl.class, null);
	}

	@Override
	public DateGraphField createDate(String key) {
		DateGraphFieldImpl field = new DateGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public DateGraphField getDate(String key) {
		if (fieldExists(key)) {
			return new DateGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NumberGraphField createNumber(String key) {
		NumberGraphFieldImpl field = new NumberGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NumberGraphField getNumber(String key) {
		if (fieldExists(key)) {
			return new NumberGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public HtmlGraphField createHTML(String key) {
		HtmlGraphFieldImpl field = new HtmlGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public HtmlGraphField getHtml(String key) {
		if (fieldExists(key)) {
			return new HtmlGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public BooleanGraphField createBoolean(String key) {
		BooleanGraphFieldImpl field = new BooleanGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public BooleanGraphField getBoolean(String key) {
		if (fieldExists(key)) {
			return new BooleanGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public <T extends ListableGraphField> SelectGraphField<T> createSelect(String key) {
		SelectGraphFieldImpl<T> field = getGraph().addFramedVertex(SelectGraphFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, HAS_FIELD);
		return field;
	}

	@Override
	public <T extends ListableGraphField> SelectGraphField<T> getSelect(String key) {
		return outE(HAS_FIELD).has(SelectGraphFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(SelectGraphFieldImpl.class, null);
	}

	@Override
	public NumberGraphFieldList createNumberList(String fieldKey) {
		return createList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NumberGraphFieldList getNumberList(String fieldKey) {
		return getList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList createNodeList(String fieldKey) {
		return createList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList getNodeList(String fieldKey) {
		return getList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList createStringList(String fieldKey) {
		return createList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList getStringList(String fieldKey) {
		return getList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList createBooleanList(String fieldKey) {
		return createList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList getBooleanList(String fieldKey) {
		return getList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicroschemaGraphFieldList createMicroschemaFieldList(String fieldKey) {
		return createList(MicroschemaGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicroschemaGraphFieldList getMicroschemaList(String fieldKey) {
		return getList(MicroschemaGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList createHTMLList(String fieldKey) {
		return createList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList getHTMLList(String fieldKey) {
		return getList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList createDateList(String fieldKey) {
		return createList(DateGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList getDateList(String fieldKey) {
		return getList(DateGraphFieldListImpl.class, fieldKey);
	}

	private <T extends ListGraphField<?,?>> T getList(Class<T> classOfT, String fieldKey) {
		return out(HAS_LIST).has(classOfT).has(GraphField.FIELD_KEY_PROPERTY_KEY, fieldKey).nextOrDefaultExplicit(classOfT, null);
	}

	private <T extends ListGraphField<?,?>> T createList(Class<T> classOfT, String fieldKey) {
		T list = getGraph().addFramedVertex(classOfT);
		list.setFieldKey(fieldKey);
		linkOut(list.getImpl(), HAS_LIST);
		return list;
	}
}
