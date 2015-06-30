package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

public class MeshEdge extends AbstractEdgeFrame {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

	public String getFermaType() {
		return getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

}
