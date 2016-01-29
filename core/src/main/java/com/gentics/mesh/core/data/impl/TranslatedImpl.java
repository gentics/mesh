package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.Translated;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.syncleus.ferma.AbstractEdgeFrame;

/**
 * @see Translated
 */
public class TranslatedImpl extends AbstractEdgeFrame implements Translated {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public MeshVertexImpl getStartNode() {
		return inV().nextOrDefault(MeshVertexImpl.class, null);
	}

	public AbstractBasicGraphFieldContainerImpl getI18NProperties() {
		return outV().nextOrDefault(AbstractBasicGraphFieldContainerImpl.class, null);
	}
}
