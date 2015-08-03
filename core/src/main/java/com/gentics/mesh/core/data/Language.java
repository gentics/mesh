package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface Language extends GenericVertex<LanguageResponse>, NamedNode {

	public static final String TYPE = "language";

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	void setLanguageTag(String languageTag);

	LanguageImpl getImpl();

}
