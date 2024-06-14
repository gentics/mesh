package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see MicroschemaRoot
 */
public class MicroschemaContainerRootImpl extends AbstractRootVertex<Microschema> implements MicroschemaRoot {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
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
	public void removeMicroschema(HibMicroschema container, EventQueueBatch batch) {
		Microschema graphMicroschema = toGraph(container);
		removeItem(graphMicroschema);
	}

	@Override
	public long globalCount() {
		return db().count(MicroschemaContainerImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The microschema container root can't be deleted");
	}

	@Override
	public boolean contains(HibMicroschema microschema) {
		return !(findByUuid(microschema.getUuid()) == null);
	}

	@Override
	public Microschema create() {
		return getGraph().addFramedVertex(MicroschemaContainerImpl.class);
	}

	@Override
	public HibProject getProject() {
		return in(HAS_MICROSCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Result<? extends MicroschemaRoot> getRoots(Microschema microschema) {
		return microschema.in(HAS_SCHEMA_CONTAINER_ITEM, MicroschemaContainerRootImpl.class);
	}
}
