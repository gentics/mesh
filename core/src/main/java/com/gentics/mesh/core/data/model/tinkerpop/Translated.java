package com.gentics.mesh.core.data.model.tinkerpop;

import org.jglue.totorom.FramedEdge;

import com.gentics.mesh.core.data.model.generic.GenericNode;

public class Translated extends FramedEdge {

	public static final String LANGUAGE_KEY = "languageTag";

	public String getLanguageTag() {
		return getProperty(LANGUAGE_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_KEY, languageTag);
	}

	public GenericNode getStartNode() {
		return inV().next(GenericNode.class);
	}

	public I18NProperties getI18NProperties() {
		return outV().next(I18NProperties.class);
	}
}
