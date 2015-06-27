package com.gentics.mesh.core.data.model.schema.propertytype;

import com.gentics.mesh.core.data.model.PropertyTypeSchema;

public interface BasicPropertyType extends PropertyTypeSchema {

	void setDescription(String description);

	String getDescription();

	String getDisplayName();

	void setDisplayName(String displayName);

	void setKey(String key);

	void setType(PropertyType type);

	String getType();

	String getKey();

}
