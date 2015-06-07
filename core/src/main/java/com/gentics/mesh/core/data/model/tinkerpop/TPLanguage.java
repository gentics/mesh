package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.Property;

public interface TPLanguage extends TPGenericNode {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName();

	@Property("languageTag")
	public String getLanguageTag();
	

}
