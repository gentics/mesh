package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.MICROSCHEMA_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static com.gentics.mesh.util.StreamUtil.uniqueBy;

import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.syncleus.ferma.ElementFrame;

/**
 * @see MicroschemaVersion
 */
public class MicroschemaContainerVersionImpl extends
	AbstractGraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschemaVersion, HibMicroschema>
	implements MicroschemaVersion {

	/**
	 * Initialize the vertex type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Class<? extends MicroschemaVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	@Override
	public Class<? extends Microschema> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public Result<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		return new TraversalResult<>(getMicronodeStream()
			.flatMap(micronode -> micronode.getContainers().stream())
			.filter(uniqueBy(ElementFrame::getId))
			.filter(container -> container.isDraft(branchUuid)));
	}

	@Override
	public Result<? extends Micronode> findMicronodes() {
		return new TraversalResult<>(getMicronodeStream());
	}

	private Stream<MicronodeImpl> getMicronodeStream() {
		return toStream(db().getVertices(
			MicronodeImpl.class,
			new String[] { MICROSCHEMA_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() })).map(v -> graph.frameElementExplicit(v, MicronodeImpl.class));
	}

	@Override
	public MicroschemaReferenceImpl transformToReference() {
		MicroschemaReferenceImpl reference = new MicroschemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		reference.setVersionUuid(getUuid());
		return reference;
	}

	@Override
	public Result<? extends HibBranch> getBranches() {
		return in(HAS_MICROSCHEMA_VERSION, BranchImpl.class);
	}

	@Override
	public Iterable<? extends HibJob> referencedJobsViaTo() {
		return in(HAS_TO_VERSION).frame(Job.class);
	}

	@Override
	public Iterable<? extends HibJob> referencedJobsViaFrom() {
		return in(HAS_FROM_VERSION).frame(Job.class);
	}

	// TODO: left here for backwards compatibility. Use DAO method where possible.
	@Override
	public MeshElementEventModel onCreated() {
		return getSchemaContainer().onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return getSchemaContainer().onUpdated();
	}

	/**
	 * Delete the graph element.
	 */
	public void deleteElement() {
		remove();
	}

}
