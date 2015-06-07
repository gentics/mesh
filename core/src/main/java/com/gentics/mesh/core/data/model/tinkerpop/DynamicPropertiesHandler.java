package com.gentics.mesh.core.data.model.tinkerpop;

import java.lang.reflect.Method;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.modules.MethodHandler;

public class DynamicPropertiesHandler implements MethodHandler<DynamicProperties> {

	@Override
	public Class<DynamicProperties> getAnnotationType() {
		return DynamicProperties.class;
	}

	@Override
	public Object processElement(Object frame, Method method, Object[] arguments, DynamicProperties annotation, FramedGraph<?> framedGraph,
			Element element) {
		if(method.getName().startsWith("get")) {
			
		}
		return null;
	}

}
