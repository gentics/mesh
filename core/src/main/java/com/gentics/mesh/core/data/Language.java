package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface Language extends GenericNode<LanguageResponse>, NamedNode {

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	void setLanguageTag(String languageTag);

	LanguageImpl getImpl();

}
