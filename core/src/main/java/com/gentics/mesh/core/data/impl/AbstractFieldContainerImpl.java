package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_LIST;

import java.util.List;

import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.impl.basic.BooleanFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.DateFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.SelectFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.field.nesting.SelectField;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

public class AbstractFieldContainerImpl extends AbstractBasicFieldContainerImpl implements FieldContainer {

	public List<String> getFieldnames() {
		return null;
	}

	@Override
	public StringField createString(String key) {
		// TODO check whether the key is already occupied
		StringFieldImpl field = new StringFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public StringField getString(String key) {
		if (fieldExists(key)) {
			return new StringFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NodeField createNode(String key, Node node) {
		NodeFieldImpl field = getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, NodeFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	public NodeField getNode(String key) {
		return outE(HAS_FIELD).has(NodeFieldImpl.class).has("field-key", key).nextOrDefaultExplicit(NodeFieldImpl.class, null);
	}

	@Override
	public DateField createDate(String key) {
		DateFieldImpl field = new DateFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public DateField getDate(String key) {
		if (fieldExists(key)) {
			return new DateFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NumberField createNumber(String key) {
		NumberFieldImpl field = new NumberFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NumberField getNumber(String key) {
		if (fieldExists(key)) {
			return new NumberFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public HtmlField createHTML(String key) {
		HtmlFieldImpl field = new HtmlFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public HtmlField getHTML(String key) {
		if (fieldExists(key)) {
			return new HtmlFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public BooleanField createBoolean(String key) {
		BooleanFieldImpl field = new BooleanFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public BooleanField getBoolean(String key) {
		if (fieldExists(key)) {
			return new BooleanFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public <T extends ListableField> SelectField<T> createSelect(String key) {
		SelectFieldImpl<T> field = getGraph().addFramedVertex(SelectFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, HAS_FIELD);
		return field;
	}

	@Override
	public <T extends ListableField> SelectField<T> getSelect(String key) {
		return outE(HAS_FIELD).has(SelectFieldImpl.class).has("fieldKey", key).nextOrDefaultExplicit(SelectFieldImpl.class, null);
	}

	@Override
	public NumberFieldList createNumberList(String fieldKey) {
		return createList(NumberFieldListImpl.class, fieldKey);
	}

	@Override
	public NumberFieldList getNumberList(String fieldKey) {
		return getList(NumberFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeFieldList createNodeList(String fieldKey) {
		return createList(NodeFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeFieldList getNodeList(String fieldKey) {
		return getList(NodeFieldListImpl.class, fieldKey);
	}

	@Override
	public StringFieldList createStringList(String fieldKey) {
		return createList(StringFieldListImpl.class, fieldKey);
	}

	@Override
	public StringFieldList getStringList(String fieldKey) {
		return getList(StringFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanFieldList createBooleanList(String fieldKey) {
		return createList(BooleanFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanFieldList getBooleanList(String fieldKey) {
		return getList(BooleanFieldListImpl.class, fieldKey);
	}

	@Override
	public MicroschemaFieldList createMicroschemaFieldList(String fieldKey) {
		return createList(MicroschemaFieldListImpl.class, fieldKey);
	}

	@Override
	public MicroschemaFieldList getMicroschemaList(String fieldKey) {
		return getList(MicroschemaFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlFieldList createHTMLList(String fieldKey) {
		return createList(HtmlFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlFieldList getHTMLList(String fieldKey) {
		return getList(HtmlFieldListImpl.class, fieldKey);
	}

	@Override
	public DateFieldList createDateList(String fieldKey) {
		return createList(DateFieldListImpl.class, fieldKey);
	}

	@Override
	public DateFieldList getDateList(String fieldKey) {
		return getList(DateFieldListImpl.class, fieldKey);
	}

	private <T extends com.gentics.mesh.core.data.node.field.list.ListField<?>> T getList(Class<T> classOfT, String fieldKey) {
		return out(HAS_LIST).has(classOfT).has("fieldKey", fieldKey).nextOrDefaultExplicit(classOfT, null);
	}

	private <T extends com.gentics.mesh.core.data.node.field.list.ListField<?>> T createList(Class<T> classOfT, String fieldKey) {
		T list = getGraph().addFramedVertex(classOfT);
		list.setFieldKey(fieldKey);
		linkOut(list.getImpl(), HAS_LIST);
		return list;
	}
}
