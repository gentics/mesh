package com.gentics.mesh.core.rest.lang;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class LanguageResponse extends AbstractRestModel {

	private String name;
	private String nativeName;
	private String languageTag;

	public String getLanguageTag() {
		return languageTag;
	}

	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNativeName() {
		return nativeName;
	}

	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}
}
