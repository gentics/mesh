package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.impl.LanguageImpl;

public interface Language extends GenericNode {

	String getName();

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	LanguageImpl getImpl();

}
