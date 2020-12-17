package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.MICROSCHEMA_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static com.gentics.mesh.util.StreamUtil.uniqueBy;

import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
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
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
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
	protected Class<? extends MicroschemaVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	@Override
	protected Class<? extends Microschema> getContainerClass() {
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
	public MicroschemaVersionModel getSchema() {
		MicroschemaVersionModel microschema = mesh().serverSchemaStorage().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			microschema = JsonUtil.readValue(getJson(), MicroschemaModelImpl.class);
			mesh().serverSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setSchema(MicroschemaVersionModel microschema) {
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
		Microschema container = toGraph(getSchemaContainer());
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

	@Override
	public void delete(BulkActionContext bac) {
		// Delete change
		SchemaChange<?> change = getNextChange();
		if (change != null) {
			change.delete(bac);
		}
		// Delete referenced jobs
		for (HibJob job : referencedJobsViaFrom()) {
			job.remove();
		}
		for (HibJob job : referencedJobsViaTo()) {
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

	/**
	 * Delete the graph element.
	 */
	public void deleteElement() {
		remove();
	}

}
