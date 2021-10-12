package com.gentics.mesh.core.data.schema;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface HibFieldSchemaElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement<R>, HibUserTracking, HibNamedElement {

	String getElementVersion();

	/**
	 * Return the latest container version.
	 * 
	 * @return Latest version
	 */
	SCV getLatestVersion();

	/**
	 * Set the latest container version.
	 * 
	 * @param version
	 */
	void setLatestVersion(SCV version);

	/**
	 * Return a map of all branches which reference the container via an assigned container version. The found container version will be added as key to the
	 * map.
	 * 
	 * @return
	 */
	Map<HibBranch, SCV> findReferencedBranches();

	R transformToRestSync(InternalActionContext ac, int level, String... languageTags);
}
