package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface HibFieldSchemaElement extends HibCoreElement, HibUserTracking, HibNamedElement {

	String getElementVersion();

}
