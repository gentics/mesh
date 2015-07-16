package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Edge;

public class MeshEdgeImpl extends AbstractEdgeFrame implements MeshEdge {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

	public MeshEdgeImpl getImpl() {
		return this;
	}

}
