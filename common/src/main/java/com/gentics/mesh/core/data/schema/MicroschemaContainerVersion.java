package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.Events.EVENT_MICROSCHEMA_CREATED;
import static com.gentics.mesh.Events.EVENT_MICROSCHEMA_UPDATED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_DELETED;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container version is a container which holds a specific microschema. Microschema versions are usually bound to a {@link MicroschemaContainer}.
 * {@link Micronode}'s which are graph field containers reference the version. The microschema is used as a blueprint by the {@link Micronode} in order to
 * correctly transform the micronode into its JSON representation.
 */
public interface MicroschemaContainerVersion extends
		GraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaModel, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer> {

	static final String TYPE = "microschemaVersion";

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_MICROSCHEMA_CREATED, EVENT_MICROSCHEMA_UPDATED, EVENT_SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return an iterator over {@link NodeGraphFieldContainer}'s that contain at least one micronode field (or list of micronodes field) that uses this schema
	 * version for the given release.
	 *
	 * @param releaseUuid
	 *            Uuid of the release
	 * @return
	 */
	Iterator<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid);

	/**
	 * Find a list of micronodes which reference this microschema version.
	 *
	 * @return List of micronodes
	 */
	List<? extends Micronode> findMicronodes();
}
