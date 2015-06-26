package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.syncleus.ferma.AbstractEdgeFrame;

public class Translated extends AbstractEdgeFrame {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public GenericNode getStartNode() {
		return inV().nextOrDefault(GenericNode.class, null);
	}

	public AbstractFieldContainer getI18NProperties() {
		return outV().nextOrDefault(AbstractFieldContainer.class, null);
	}
}
