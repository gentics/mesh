package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface Language extends GenericNode<LanguageResponse> {

	String getName();

	void setName(String name);

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	void setLanguageTag(String languageTag);

	LanguageImpl getImpl();

}
