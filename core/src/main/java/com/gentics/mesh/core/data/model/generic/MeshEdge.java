package com.gentics.mesh.core.data.model.generic;

import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.AbstractEdgeFrame;

public class MeshEdge extends AbstractEdgeFrame {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

}
