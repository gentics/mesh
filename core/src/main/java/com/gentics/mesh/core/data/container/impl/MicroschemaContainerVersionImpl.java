package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.ETag;

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
	public Iterator<? extends NodeGraphFieldContainer> getDraftFieldContainers(String releaseUuid) {
		Iterator<? extends NodeGraphFieldContainer> it = in(HAS_MICROSCHEMA_CONTAINER)
				.copySplit(
						(a) -> a.in(HAS_FIELD).mark().inE(HAS_FIELD_CONTAINER)
								.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode())
								.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).back(),
						(a) -> a.in(HAS_ITEM).in(HAS_LIST).mark().inE(HAS_FIELD_CONTAINER)
								.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode())
								.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).back())
				.fairMerge().dedup().transform(v -> v.reframeExplicit(NodeGraphFieldContainerImpl.class)).iterator();
		return it;
	}

	@Override
	public Iterator<? extends Micronode> findMicronodes() {
		return in(HAS_MICROSCHEMA_CONTAINER).frameExplicit(MicronodeImpl.class).iterator();
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
		String json = JsonUtil.toJson(microschema);
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, microschema.getVersion());
	}

	@Override
	public MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		// Load the microschema and add/overwrite some properties
		MicroschemaResponse microschema = JsonUtil.readValue(getJson(), MicroschemaResponse.class);
		// Role permissions
		MicroschemaContainer container = getSchemaContainer();
		container.setRolePermissions(ac, microschema);
		container.fillCommonRestFields(ac, microschema);

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
	public List<? extends Release> getReleases() {
		return in(HAS_MICROSCHEMA_VERSION).toListExplicit(ReleaseImpl.class);
	}

	@Override
	public Single<MicroschemaResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().operateTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public void onCreated() {
		getSchemaContainer().onCreated();
	}

	@Override
	public void onUpdated() {
		getSchemaContainer().onUpdated();
	}

}
