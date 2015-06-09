package com.gentics.mesh.core.data.model.tinkerpop;

import org.jglue.totorom.FramedEdge;

import com.gentics.mesh.core.data.model.generic.GenericNode;

public class Translated extends FramedEdge {

	public String getLanguageTag() {
		return getProperty("languageTag");
	}

//	@InVertex
	public GenericNode getStartNode() {
		return inV().frame(GenericNode.class);
	}
//
//	@OutVertex
	public I18NProperties getI18NProperties() {
		return outV().frame(I18NProperties.class);
	}
}
