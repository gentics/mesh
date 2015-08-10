package com.gentics.mesh.core.data.generic;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.VertexFrame;
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

	@SuppressWarnings("unchecked")
	public Object getId() {
		return getElement().getId();
	}

	public void setLinkInTo(VertexFrame vertex, String... labels) {
		unlinkIn(vertex, labels);
		linkIn(vertex, labels);
	}

	public void setLinkOutTo(VertexFrame vertex, String... labels) {

		// Unlink all edges between both objects with the given label
		// if (out(labels).retain(vertex).hasNext()) {
		unlinkOut(vertex, labels);
		// }
		// Create a new edge with the given label
		linkOut(vertex, labels);
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

	@Override
	public void delete() {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}
}
