package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.VertexFrame;

import io.reactivex.Single;

public class MicroschemaContainerVersionImpl extends
	AbstractGraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaModel, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer>
	implements MicroschemaContainerVersion {

	public static void init(Database database) {
		database.addVertexType(MicroschemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	protected Class<? extends MicroschemaContainerVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	@Override
	protected Class<? extends MicroschemaContainer> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public TraversalResult<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		Iterator<? extends NodeGraphFieldContainer> it = in(HAS_MICROSCHEMA_CONTAINER).copySplit((a) -> a.in(HAS_FIELD).mark().inE(
			HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()).has(
				GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid)
			.back(),
			(a) -> a.in(HAS_ITEM).in(HAS_LIST).mark().inE(
				HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()).has(
					GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid)
				.back())
			.fairMerge()
			// To circumvent a bug in the ferma library we have to transform the VertexFrame object to itself
			// before calling dedup(). This forces the actual conversion to VertexFrame inside of the pipeline.
			.transform(v -> v)
			// when calling dedup we use the the id of the vertex instead of the whole object to save memory
			.dedup(VertexFrame::getId).transform(v -> v.reframeExplicit(NodeGraphFieldContainerImpl.class)).iterator();
		return new TraversalResult<>(() -> it);
	}

	@Override
	public TraversalResult<? extends Micronode> findMicronodes() {
		return new TraversalResult<>(in(HAS_MICROSCHEMA_CONTAINER).frameExplicit(MicronodeImpl.class));
	}

	@Override
	public MicroschemaModel getSchema() {
		MicroschemaModel microschema = MeshInternal.get().serverSchemaStorage().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			microschema = JsonUtil.readValue(getJson(), MicroschemaModelImpl.class);
			MeshInternal.get().serverSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setSchema(MicroschemaModel microschema) {
		MeshInternal.get().serverSchemaStorage().removeMicroschema(microschema.getName(), microschema.getVersion());
		MeshInternal.get().serverSchemaStorage().addMicroschema(microschema);
		String json = microschema.toJson();
		setJson(json);
		property(VERSION_PROPERTY_KEY, microschema.getVersion());
	}

	@Override
	public MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		// Load the microschema and add/overwrite some properties
		MicroschemaResponse microschema = JsonUtil.readValue(getJson(), MicroschemaResponse.class);
		// TODO apply fields filtering here

		// Role permissions
		MicroschemaContainer container = getSchemaContainer();
		container.setRolePermissions(ac, microschema);
		container.fillCommonRestFields(ac, fields, microschema);

		return microschema;
	}

	@Override
	public MicroschemaReferenceImpl transformToReference() {
		MicroschemaReferenceImpl reference = new MicroschemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		return reference;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Branch> getBranches() {
		return in(HAS_MICROSCHEMA_VERSION).toListExplicit(BranchImpl.class);
	}

	@Override
	public Iterable<Job> referencedJobsViaTo() {
		return in(HAS_TO_VERSION).frame(Job.class);
	}

	@Override
	public Iterable<Job> referencedJobsViaFrom() {
		return in(HAS_FROM_VERSION).frame(Job.class);
	}

	@Override
	public Single<MicroschemaResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public void delete(BulkActionContext bac) {
		// Delete change
		SchemaChange<?> change = getNextChange();
		if (change != null) {
			change.delete(bac);
		}
		// Delete referenced jobs
		for (Job job : referencedJobsViaFrom()) {
			job.remove();
		}
		for (Job job : referencedJobsViaTo()) {
			job.remove();
		}
		// Delete version
		remove();

	}

	@Override
	public MeshElementEventModel onCreated() {
		return getSchemaContainer().onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return getSchemaContainer().onUpdated();
	}

}
