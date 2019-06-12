package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.madl.TraversalResult;

import static com.gentics.mesh.ElementType.MICROSCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;

/**
 * A microschema container version is a container which holds a specific microschema. Microschema versions are usually bound to a {@link MicroschemaContainer}.
 * {@link Micronode}'s which are graph field containers reference the version. The microschema is used as a blueprint by the {@link Micronode} in order to
 * correctly transform the micronode into its JSON representation.
 */
public interface MicroschemaContainerVersion extends
		GraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaModel, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer> {

	static final TypeInfo TYPE_INFO = new TypeInfo(MICROSCHEMAVERSION, MICROSCHEMA_CREATED, MICROSCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return an iterator over all draft {@link NodeGraphFieldContainer}'s that contain at least one micronode field (or list of micronodes field) that uses
	 * this schema version for the given branch.
	 *
	 * @param branchUuid
	 *            Uuid of the branch
	 * @return
	 */
	TraversalResult<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid);

	/**
	 * Return an iterator over micronodes which reference this microschema version.
	 *
	 * @return Iterator over micronodes
	 */
	TraversalResult<? extends Micronode> findMicronodes();

}
