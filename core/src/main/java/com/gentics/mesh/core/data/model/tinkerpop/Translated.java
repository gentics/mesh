package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import static com.gentics.mesh.util.TraversalHelper.nextOrNull;
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
		return nextOrNull(inV(), GenericNode.class);
	}

	public I18NProperties getI18NProperties() {
		return nextOrNull(outV(), I18NProperties.class);
	}
}
