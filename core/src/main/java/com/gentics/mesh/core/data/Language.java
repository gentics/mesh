package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.LanguageImpl;

public interface Language extends GenericNode {

	String getName();

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	LanguageImpl getImpl();

}
