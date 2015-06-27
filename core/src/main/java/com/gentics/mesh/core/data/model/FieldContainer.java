package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;

public interface FieldContainer extends MeshVertex {

	void setLanguage(Language language);

	void setProperty(String key, String value);

	String getProperty(String string);

}
