package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.result.Result;

/**
 * A {@link GraphFieldSchemaContainerVersion} stores the versioned data for a {@link GraphFieldSchemaContainer} element.
 * 
 * @param <R>
 *            Rest model response type
 * @param <RM><
 *            Rest model type
 * @param <RE>
 *            Reference model type
 * @param <SCV>
 *            Schema container version type
 * @param <SC>
 *            Schema container type
 * 
 */
public interface GraphFieldSchemaContainerVersion<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>
	> extends MeshCoreVertex<R>, ReferenceableElement<RE>, Comparable<SCV>, HibFieldSchemaVersionElement<R, RM, RE, SC, SCV> {

	public static final String VERSION_PROPERTY_KEY = "version";

	/**
	 * Get the branches to which the container was assigned.
	 *
	 * @return Found branches of this version
	 */
	Result<? extends HibBranch> getBranches();
}
