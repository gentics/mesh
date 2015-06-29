package com.gentics.mesh.core.data.model.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD;

import java.util.List;

import com.gentics.mesh.core.data.model.FieldContainer;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.model.node.field.basic.DateField;
import com.gentics.mesh.core.data.model.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.model.node.field.basic.NumberField;
import com.gentics.mesh.core.data.model.node.field.basic.StringField;
import com.gentics.mesh.core.data.model.node.field.impl.basic.BooleanFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.basic.DateFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.basic.HTMLFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.basic.NumberFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.basic.StringFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.nesting.ListFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.nesting.NodeFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.nesting.SelectFieldImpl;
import com.gentics.mesh.core.data.model.node.field.nesting.ListField;
import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.model.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.model.node.field.nesting.SelectField;

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
	public NodeField createNode(String key, MeshNode node) {
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
	public HTMLField createHTML(String key) {
		HTMLFieldImpl field = new HTMLFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public HTMLField getHTML(String key) {
		if (fieldExists(key)) {
			return new HTMLFieldImpl(key, this);
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
	public <T extends ListableField> ListField<T> createList(String key) {
		ListFieldImpl<T> field = getGraph().addFramedVertex(ListFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, HAS_FIELD);
		return field;
	}

	@Override
	public <T extends ListableField> ListField<T> getList(String key) {
		//		for (VertexFrame edge : out(HAS_FIELD).has(ListFieldImpl.class).toList()) {
		//			for (String key2 : edge.getPropertyKeys()) {
		//				System.out.println("V:" + key2 + " " + edge.getProperty(key2));
		//			}
		//		}
		return out(HAS_FIELD).has(ListFieldImpl.class).has("fieldKey", key).nextOrDefaultExplicit(ListFieldImpl.class, null);
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
}
