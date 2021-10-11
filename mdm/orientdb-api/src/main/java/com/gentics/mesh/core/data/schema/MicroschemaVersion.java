package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;

/**
 * A microschema container version is a container which holds a specific microschema. Microschema versions are usually bound to a {@link Microschema}.
 * {@link Micronode}'s which are graph field containers reference the version. The microschema is used as a blueprint by the {@link Micronode} in order to
 * correctly transform the micronode into its JSON representation.
 */
public interface MicroschemaVersion extends
		GraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschemaVersion, HibMicroschema>, HibMicroschemaVersion {

	/**
	 * Return an iterator over all draft {@link HibNodeFieldContainer}'s that contain at least one micronode field (or list of micronodes field) that uses
	 * this schema version for the given branch.
	 *
	 * @param branchUuid
	 *            Uuid of the branch
	 * @return
	 */
	Result<? extends HibNodeFieldContainer> getDraftFieldContainers(String branchUuid);

	@Override
	default TypeInfo getTypeInfo() {
		return HibMicroschemaVersion.super.getTypeInfo();
	}
}
