package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.MeshEvent.SCHEMA_UPDATED;

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

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, SCHEMA_CREATED.address, SCHEMA_UPDATED.address, SCHEMA_DELETED.address);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<NodeGraphFieldContainer> getFieldContainers(String branchUuid);

	/**
	 * Returns an iterator for those {@link NodeGraphFieldContainer}'s which can be edited by users. Those are draft and publish versions.
	 *
	 * @param branchUuid Branch Uuid
	 * @return
	 */
	Iterator<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid);

	/**
	 * Returns all nodes that the user has read permissions for.
	 *
	 * @param branchUuid Branch uuid
	 * @param user User to check permissions for
	 * @param type Container type
	 * @return
	 */
	Iterable<? extends Node> getNodes(String branchUuid, User user, ContainerType type);

}
