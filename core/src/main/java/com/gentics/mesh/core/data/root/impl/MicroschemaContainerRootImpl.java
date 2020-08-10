package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see MicroschemaRoot
 */
public class MicroschemaContainerRootImpl extends AbstractRootVertex<Microschema> implements MicroschemaRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_ITEM));
		index.createIndex(edgeIndex(HAS_SCHEMA_CONTAINER_ITEM).withInOut().withOut());
	}

	@Override
	public Class<? extends Microschema> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_SCHEMA_CONTAINER_ITEM;
	}

	@Override
	public void addMicroschema(HibUser user, Microschema container, EventQueueBatch batch) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(Microschema container, EventQueueBatch batch) {
		removeItem(container);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The microschema container root can't be deleted");
	}

	@Override
	public boolean contains(Microschema microschema) {
		if (findByUuid(microschema.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Microschema create() {
		return getGraph().addFramedVertex(MicroschemaContainerImpl.class);
	}

	@Override
	public MicroschemaVersion createVersion() {
		return getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
	}

	@Override
	public Microschema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public Microschema create(InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

}
