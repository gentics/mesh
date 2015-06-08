package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;
import com.tinkerpop.frames.Property;

public interface Translated extends EdgeFrame {

	@Property("languageTag")
	public String getLanguageTag();

	@InVertex
	public GenericNode getStartNode();

	@OutVertex
	public I18NProperties getI18NProperties();
}
