package com.gentics.mesh.core.data.model.node;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD;

import java.util.List;

import com.gentics.mesh.core.data.model.AbstractFieldContainer;
import com.gentics.mesh.core.data.model.node.field.BooleanField;
import com.gentics.mesh.core.data.model.node.field.DateField;
import com.gentics.mesh.core.data.model.node.field.HTMLField;
import com.gentics.mesh.core.data.model.node.field.ListField;
import com.gentics.mesh.core.data.model.node.field.MicroschemaField;
import com.gentics.mesh.core.data.model.node.field.NodeField;
import com.gentics.mesh.core.data.model.node.field.NumberField;
import com.gentics.mesh.core.data.model.node.field.StringField;
import com.syncleus.ferma.traversals.EdgeTraversal;

public class MeshNodeFieldContainer extends AbstractFieldContainer {

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
		return outE(HAS_FIELD).has(NodeField.class).has("key", key).nextOrDefaultExplicit(NodeField.class, null);
	}

	public StringField createString(String key) {
		// TODO check whether the key is already occupied
		return new StringField(key, this);
	}

	public NodeField createNode(String key, MeshNode node) {
		return getGraph().addFramedEdge(this, node, HAS_FIELD, NodeField.class);
	}

	public DateField createDate(String key) {
		DateField field = new DateField(key, this);
		return field;
	}

	public NumberField createNumber(String key) {
		NumberField field = new NumberField(key, this);
		return field;
	}

	public HTMLField createHTML(String key) {
		HTMLField field = new HTMLField(key, this);
		return field;
	}

	public BooleanField createBoolean(String key) {
		BooleanField field = new BooleanField(key, this);
		return field;
	}

	public MicroschemaField createMicroschema(String key) {
		MicroschemaField field = getGraph().addFramedVertex(MicroschemaField.class);
		linkOut(field, HAS_FIELD);
		return field;
	}
}
