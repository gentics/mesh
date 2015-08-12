package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;

import java.util.List;

import com.gentics.mesh.core.data.FieldContainer;
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
import com.gentics.mesh.core.data.node.field.impl.nesting.GraphNodeFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.GraphSelectFieldImpl;
import com.gentics.mesh.core.data.node.field.list.GraphBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.GraphBooleanFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphDateFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphHtmlFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphMicroschemaFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphNodeFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphNumberFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.GraphStringFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.field.nesting.GraphSelectField;

public abstract class AbstractFieldContainerImpl extends AbstractBasicFieldContainerImpl implements FieldContainer {

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
	public GraphNodeField createNode(String key, Node node) {
		GraphNodeFieldImpl field = getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, GraphNodeFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public GraphNodeField getNode(String key) {
		return outE(HAS_FIELD).has(GraphNodeFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(GraphNodeFieldImpl.class, null);
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
	public <T extends ListableGraphField> GraphSelectField<T> createSelect(String key) {
		GraphSelectFieldImpl<T> field = getGraph().addFramedVertex(GraphSelectFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, HAS_FIELD);
		return field;
	}

	@Override
	public <T extends ListableGraphField> GraphSelectField<T> getSelect(String key) {
		return outE(HAS_FIELD).has(GraphSelectFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(GraphSelectFieldImpl.class, null);
	}

	@Override
	public GraphNumberFieldList createNumberList(String fieldKey) {
		return createList(GraphNumberFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphNumberFieldList getNumberList(String fieldKey) {
		return getList(GraphNumberFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphNodeFieldList createNodeList(String fieldKey) {
		return createList(GraphNodeFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphNodeFieldList getNodeList(String fieldKey) {
		return getList(GraphNodeFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphStringFieldList createStringList(String fieldKey) {
		return createList(GraphStringFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphStringFieldList getStringList(String fieldKey) {
		return getList(GraphStringFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphBooleanFieldList createBooleanList(String fieldKey) {
		return createList(GraphBooleanFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphBooleanFieldList getBooleanList(String fieldKey) {
		return getList(GraphBooleanFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphMicroschemaFieldList createMicroschemaFieldList(String fieldKey) {
		return createList(GraphMicroschemaFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphMicroschemaFieldList getMicroschemaList(String fieldKey) {
		return getList(GraphMicroschemaFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphHtmlFieldList createHTMLList(String fieldKey) {
		return createList(GraphHtmlFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphHtmlFieldList getHTMLList(String fieldKey) {
		return getList(GraphHtmlFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphDateFieldList createDateList(String fieldKey) {
		return createList(GraphDateFieldListImpl.class, fieldKey);
	}

	@Override
	public GraphDateFieldList getDateList(String fieldKey) {
		return getList(GraphDateFieldListImpl.class, fieldKey);
	}

	private <T extends com.gentics.mesh.core.data.node.field.list.GraphListField<?>> T getList(Class<T> classOfT, String fieldKey) {
		return out(HAS_LIST).has(classOfT).has(GraphField.FIELD_KEY_PROPERTY_KEY, fieldKey).nextOrDefaultExplicit(classOfT, null);
	}

	private <T extends com.gentics.mesh.core.data.node.field.list.GraphListField<?>> T createList(Class<T> classOfT, String fieldKey) {
		T list = getGraph().addFramedVertex(classOfT);
		list.setFieldKey(fieldKey);
		linkOut(list.getImpl(), HAS_LIST);
		return list;
	}
}
