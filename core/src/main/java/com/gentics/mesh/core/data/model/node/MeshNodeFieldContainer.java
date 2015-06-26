package com.gentics.mesh.core.data.model.node;

import java.util.List;

import com.gentics.mesh.core.data.model.AbstractFieldContainer;
import com.gentics.mesh.core.data.model.node.field.BooleanFieldProperty;
import com.gentics.mesh.core.data.model.node.field.DateFieldProperty;
import com.gentics.mesh.core.data.model.node.field.HTMLFieldProperty;
import com.gentics.mesh.core.data.model.node.field.ListFieldProperty;
import com.gentics.mesh.core.data.model.node.field.MicroschemaFieldProperty;
import com.gentics.mesh.core.data.model.node.field.NodeFieldProperty;
import com.gentics.mesh.core.data.model.node.field.NumberFieldProperty;
import com.gentics.mesh.core.data.model.node.field.StringFieldProperty;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.syncleus.ferma.traversals.EdgeTraversal;

public class MeshNodeFieldContainer extends AbstractFieldContainer {

	public List<String> getFieldnames() {
		return null;
	}

	public ListFieldProperty getListFieldProperty(String key) {
		return null;
	}

	public MicroschemaFieldProperty getMicroschemaFieldProperty(String key) {
		return null;
	}

	public NodeFieldProperty getNodeFieldProperty(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(MeshRelationships.HAS_FIELD).has(NodeFieldProperty.class).has("key", key);
		if (traversal.hasNext()) {
			return traversal.nextExplicit(NodeFieldProperty.class);
		} else {
			return null;
		}
	}

	public StringFieldProperty createString(String key) {
		//TODO check whether the key is already occupied
		return new StringFieldProperty(key, this);
	}

	public NodeFieldProperty createNode(String key, MeshNode node) {
		return getGraph().addFramedEdge(this, node, MeshRelationships.HAS_FIELD, NodeFieldProperty.class);
	}

	public DateFieldProperty createDate(String key) {
		return null;
	}

	public NumberFieldProperty createNumber(String key) {
		return null;
	}

	public HTMLFieldProperty createHTML(String key) {
		return null;
	}

	public BooleanFieldProperty createBoolean(String key) {
		return null;
	}

	public MicroschemaFieldProperty createMicroschema(String key) {
		return null;
	}
}
