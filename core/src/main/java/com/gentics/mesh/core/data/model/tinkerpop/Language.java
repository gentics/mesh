package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.tinkerpop.frames.Property;

public interface Language extends GenericNode {

	//TODO add index
	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Property("nativeName")
	public String getNativeName();

	@Property("nativeName")
	public void setNativeName(String name);

	@Property("languageTag")
	public String getLanguageTag();

}
