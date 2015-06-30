package com.gentics.mesh.core.data.generic;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Vertex;

public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	public Map<String, String> getProperties(String prefix) {
		Map<String, String> properties = new HashMap<>();

		for (String key : getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				properties.put(key, getProperty(key));
			}
		}
		return properties;
	}

	public Object getId() {
		return getElement().getId();
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

	public Vertex getVertex() {
		return getElement();
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	@Override
	public MeshVertexImpl getImpl() {
		return this;
	}
}
