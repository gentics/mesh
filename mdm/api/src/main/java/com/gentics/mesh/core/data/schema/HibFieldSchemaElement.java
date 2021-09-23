package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface HibFieldSchemaElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement<R>, HibUserTracking, HibNamedElement<R> {

	String getElementVersion();

	SCV getLatestVersion();

	void setLatestVersion(SCV nextVersion);

}
