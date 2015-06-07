package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;
import com.tinkerpop.frames.Property;

public interface TPTranslated extends EdgeFrame {

	@Property("languageTag")
	public String getLanguageTag();

	@InVertex
	public TPGenericNode getStartNode();

	@OutVertex
	public TPI18NProperties getI18NProperties();
}
