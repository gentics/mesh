package com.gentics.mesh.core.data.model.generic;

import org.jglue.totorom.FramedEdge;

import com.gentics.mesh.util.UUIDUtil;

public class MeshEdge extends FramedEdge {

	@Override
	protected void init() {
		super.init();
		setProperty("uuid", UUIDUtil.randomUUID());
	}

}
