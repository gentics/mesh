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

	public StringField createString(String key) {
		// TODO check whether the key is already occupied
		return new StringFieldImpl(key, this);
	}

	public NodeField createNode(String key, MeshNode node) {
		return getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, NodeFieldImpl.class);
	}

	public DateField createDate(String key) {
		DateFieldImpl field = new DateFieldImpl(key, this);
		return field;
	}

	public NumberField createNumber(String key) {
		NumberFieldImpl field = new NumberFieldImpl(key, this);
		return field;
	}

	public HTMLField createHTML(String key) {
		HTMLFieldImpl field = new HTMLFieldImpl(key, this);
		return field;
	}

	public BooleanField createBoolean(String key) {
		BooleanFieldImpl field = new BooleanFieldImpl(key, this);
		return field;
	}

	public MicroschemaField createMicroschema(String key) {
		MicroschemaFieldImpl field = getGraph().addFramedVertex(MicroschemaFieldImpl.class);
		linkOut(field, HAS_FIELD);
		return field;
	}
}
