package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.Events.EVENT_SCHEMA_CREATED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_DELETED;
import static com.gentics.mesh.Events.EVENT_SCHEMA_UPDATED;

import java.util.Iterator;
import java.util.stream.Stream;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface SchemaContainerVersion
		extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaModel, SchemaReference, SchemaContainerVersion, SchemaContainer> {

	static final String TYPE = "schemaVersion";

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_SCHEMA_CREATED, EVENT_SCHEMA_UPDATED, EVENT_SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given release.
	 *
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	Stream<NodeGraphFieldContainer> getFieldContainers(String releaseUuid);

	/**
	 * Returns an iterator for those {@link NodeGraphFieldContainer}'s which can be edited by users. Those are draft and publish versions.
	 *
	 * @param releaseUuid Release Uuid
	 * @return
	 */
	Iterator<? extends NodeGraphFieldContainer> getDraftFieldContainers(String releaseUuid);

	/**
	 * Returns all nodes that the user has read permissions for.
	 *
	 * @param releaseUuid Release uuid
	 * @param user User to check permissions for
	 * @param type Container type
	 * @return
	 */
	Iterable<? extends Node> getNodes(String releaseUuid, User user, ContainerType type);

}
