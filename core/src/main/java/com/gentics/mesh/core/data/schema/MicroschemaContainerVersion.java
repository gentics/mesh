package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container version is a container which holds a specific microschema. Microschema versions are usually bound to a {@link MicroschemaContainer}.
 * {@link Micronode}'s which are graph field containers reference the version. The microschema is used as a blueprint by the {@link Micronode} in order to
 * correctly transform the micronode into its JSON representation.
 */
public interface MicroschemaContainerVersion
		extends GraphFieldSchemaContainerVersion<Microschema, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer> {

	/**
	 * Return a list {@link NodeGraphFieldContainer} that contain at least one micronode field (or list of micronodes field) that uses this schema version for
	 * the given release.
	 *
	 * @param releaseUuid
	 *            Uuid of the release
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid);

	/**
	 * Find a list of micronodes which reference this microschema version.
	 *
	 * @return List of micronodes
	 */
	List<? extends Micronode> findMicronodes();
}
