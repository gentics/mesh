package com.gentics.mesh.core.data.model;


public interface FieldContainer extends MeshVertex {

	void setLanguage(Language language);

	void setI18nProperty(String key, String value);

	String getI18nProperty(String string);

}
