package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;

public class Language extends GenericNode {

	//TODO add index
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getNativeName() {
		return getProperty("nativeName");
	}

	public void setNativeName(String name) {
		setProperty("nativeName", name);
	}

	public String getLanguageTag() {
		return getProperty("languageTag");
	}

	public void setLanguageTag(String languageTag) {
		setProperty("languageTag", languageTag);
	}

}
