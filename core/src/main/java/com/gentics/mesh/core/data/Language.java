package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;

public interface Language extends GenericNode {

	String getName();

	void setName(String name);

	void setNativeName(String languageNativeName);

	String getNativeName();

	String getLanguageTag();

	void setLanguageTag(String languageTag);

	LanguageImpl getImpl();

}
