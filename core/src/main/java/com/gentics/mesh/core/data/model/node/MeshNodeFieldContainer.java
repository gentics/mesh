package com.gentics.mesh.core.data.model.node;

import java.util.List;

import com.gentics.mesh.core.data.model.AbstractFieldContainer;
import com.gentics.mesh.core.data.model.node.field.ListFieldProperty;
import com.gentics.mesh.core.data.model.node.field.MicroschemaFieldProperty;
import com.gentics.mesh.core.data.model.node.field.NodeFieldProperty;

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
		return null;
	}

}
