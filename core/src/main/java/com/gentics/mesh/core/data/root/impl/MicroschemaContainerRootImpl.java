package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see MicroschemaContainerRoot
 */
public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_ITEM));
		index.createIndex(edgeIndex(HAS_SCHEMA_CONTAINER_ITEM).withInOut().withOut());
	}

	@Override
	public Class<? extends MicroschemaContainer> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_SCHEMA_CONTAINER_ITEM;
	}

	@Override
	public void addMicroschema(User user, MicroschemaContainer container, EventQueueBatch batch) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(MicroschemaContainer container, EventQueueBatch batch) {
		removeItem(container);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The microschema container root can't be deleted");
	}

	@Override
	public boolean contains(MicroschemaContainer microschema) {
		if (findByUuid(microschema.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public MicroschemaContainer create() {
		return getGraph().addFramedVertex(MicroschemaContainerImpl.class);
	}

	@Override
	public MicroschemaContainerVersion createVersion() {
		return getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
	}

	@Override
	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

}
