package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;

public class LanguageImpl extends AbstractGenericNode implements Language {

	// TODO add index
	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getNativeName() {
		return getProperty("nativeName");
	}

	@Override
	public void setNativeName(String name) {
		setProperty("nativeName", name);
	}

	@Override
	public String getLanguageTag() {
		return getProperty("languageTag");
	}

	@Override
	public void setLanguageTag(String languageTag) {
		setProperty("languageTag", languageTag);
	}
	
	
	@Override
	public LanguageImpl getImpl() {
		return this;
	}
}
