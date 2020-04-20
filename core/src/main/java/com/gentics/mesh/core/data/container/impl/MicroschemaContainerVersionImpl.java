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
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.syncleus.ferma.ElementFrame;

import io.reactivex.Single;

public class MicroschemaContainerVersionImpl extends
	AbstractGraphFieldSchemaContainerVersion<MicroschemaResponse, MicroschemaModel, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer>
	implements MicroschemaContainerVersion {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicroschemaContainerVersionImpl.class, MeshVertexImpl.class);
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
	public TraversalResult<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		return new TraversalResult<>(getMicronodeStream()
			.flatMap(micronode -> micronode.getContainers().stream())
			.filter(uniqueBy(ElementFrame::getId))
			.filter(container -> container.isDraft(branchUuid)));
	}

	@Override
	public TraversalResult<? extends Micronode> findMicronodes() {
		return new TraversalResult<>(getMicronodeStream());
	}

	private Stream<MicronodeImpl> getMicronodeStream() {
		return toStream(db().getVertices(
			MicronodeImpl.class,
			new String[]{MICROSCHEMA_VERSION_KEY_PROPERTY},
			new Object[]{getUuid()}
		)).map(v -> graph.frameElementExplicit(v, MicronodeImpl.class));
	}

	@Override
	public MicroschemaModel getSchema() {
		MicroschemaModel microschema = mesh().serverSchemaStorage().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			microschema = JsonUtil.readValue(getJson(), MicroschemaModelImpl.class);
			mesh().serverSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setSchema(MicroschemaModel microschema) {
		mesh().serverSchemaStorage().removeMicroschema(microschema.getName(), microschema.getVersion());
		mesh().serverSchemaStorage().addMicroschema(microschema);
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
		microschema.setRolePerms(container.getRolePermissions(ac, ac.getRolePermissionParameters().getRoleUuid()));
		container.fillCommonRestFields(ac, fields, microschema);

		return microschema;
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
	public String getSubETag(InternalActionContext ac) {
		return "";
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TraversalResult<? extends Branch> getBranches() {
		return in(HAS_MICROSCHEMA_VERSION, BranchImpl.class);
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
