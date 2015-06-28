package com.gentics.mesh.core.data.model.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.field.BooleanField;
import com.gentics.mesh.core.data.model.node.field.DateField;
import com.gentics.mesh.core.data.model.node.field.HTMLField;
import com.gentics.mesh.core.data.model.node.field.ListField;
import com.gentics.mesh.core.data.model.node.field.MicroschemaField;
import com.gentics.mesh.core.data.model.node.field.NodeField;
import com.gentics.mesh.core.data.model.node.field.NumberField;
import com.gentics.mesh.core.data.model.node.field.StringField;
import com.gentics.mesh.core.data.model.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.HTMLFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.data.model.node.field.impl.StringFieldImpl;

public class MeshNodeFieldContainerImpl extends AbstractFieldContainerImpl implements MeshNodeFieldContainer {

	public List<String> getFieldnames() {
		return null;
	}

	public ListField getListFieldProperty(String key) {
		return null;
	}

	public MicroschemaField getMicroschemaFieldProperty(String key) {
		return null;
	}

	public NodeField getNodeFieldProperty(String key) {
		return outE(HAS_FIELD).has(NodeFieldImpl.class).has("key", key).nextOrDefaultExplicit(NodeFieldImpl.class, null);
	}

	@Override
	public StringField createString(String key) {
		// TODO check whether the key is already occupied
		StringFieldImpl field = new StringFieldImpl(key, this);
		field.setFieldKey();
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
	public BooleanField getBoolean(String key) {
		if (fieldExists(key)) {
			return new BooleanFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NodeField createNode(String key, MeshNode node) {
		return getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, NodeFieldImpl.class);
	}

	@Override
	public DateField createDate(String key) {
		DateFieldImpl field = new DateFieldImpl(key, this);
		field.setFieldKey();
		return field;
	}

	public DateField getDate(String key) {
		if (fieldExists(key)) {
			return new DateFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NumberField createNumber(String key) {
		NumberFieldImpl field = new NumberFieldImpl(key, this);
		field.setFieldKey();
		return field;
	}

	public NumberField getNumber(String key) {
		if (fieldExists(key)) {
			return new NumberFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public HTMLField createHTML(String key) {
		HTMLFieldImpl field = new HTMLFieldImpl(key, this);
		field.setFieldKey();
		return field;
	}

	public HTMLField getHTML(String key) {
		if (fieldExists(key)) {
			return new HTMLFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public BooleanField createBoolean(String key) {
		BooleanFieldImpl field = new BooleanFieldImpl(key, this);
		field.setFieldKey();
		return field;
	}

	@Override
	public MicroschemaField createMicroschema(String key) {
		MicroschemaFieldImpl field = getGraph().addFramedVertex(MicroschemaFieldImpl.class);
		linkOut(field, HAS_FIELD);
		return field;
	}

}
