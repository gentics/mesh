package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.user.HibUserTracking;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface HibFieldSchemaElement extends HibCoreElement, HibUserTracking {

	String getName();

	void setName(String name);

	String getElementVersion();

}
