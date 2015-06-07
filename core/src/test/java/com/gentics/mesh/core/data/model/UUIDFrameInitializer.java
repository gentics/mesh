package com.gentics.mesh.core.data.model;

import com.gentics.mesh.util.UUIDUtil;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.frames.FrameInitializer;
import com.tinkerpop.frames.FramedGraph;

public class UUIDFrameInitializer implements FrameInitializer {

	@Override
	public void initElement(Class<?> kind, FramedGraph<?> framedGraph, Element element) {
		element.setProperty("uuid", UUIDUtil.randomUUID());
	}

}
