package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.RestModelHelper;

import rx.Single;

public class MicroschemaContainerVersionImpl
		extends AbstractGraphFieldSchemaContainerVersion<Microschema, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer>
		implements MicroschemaContainerVersion {

	public static void init(Database database) {
		database.addVertexType(MicroschemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public MicroschemaReference createEmptyReferenceModel() {
		return new MicroschemaReference();
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
	protected String getMigrationAddress() {
		return NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid) {
		return in(HAS_MICROSCHEMA_CONTAINER).has(MicronodeImpl.class).copySplit(
				(a) -> a.in(HAS_FIELD).mark().inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode())
						.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).back(),
				(a) -> a.in(HAS_ITEM).in(HAS_LIST).mark().inE(HAS_FIELD_CONTAINER)
						.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode())
						.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).back())
				.fairMerge().dedup().transform(v -> v.reframeExplicit(NodeGraphFieldContainerImpl.class)).toList();
	}

	@Override
	public Microschema getSchema() {
		Microschema microschema = MeshInternal.get().serverSchemaStorage().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			try {
				microschema = JsonUtil.readValue(getJson(), MicroschemaModel.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			MeshInternal.get().serverSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setSchema(Microschema microschema) {
		MeshInternal.get().serverSchemaStorage().removeMicroschema(microschema.getName(), microschema.getVersion());
		MeshInternal.get().serverSchemaStorage().addMicroschema(microschema);
		String json = JsonUtil.toJson(microschema);
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, microschema.getVersion());
	}

	@Override
	public Single<Microschema> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		try {
			// Load the microschema and add/overwrite some properties 
			Microschema microschema = JsonUtil.readValue(getJson(), MicroschemaModel.class);
			microschema.setUuid(getSchemaContainer().getUuid());

			// Role permissions
			RestModelHelper.setRolePermissions(ac, getSchemaContainer(), microschema);
			microschema.setPermissions(ac.getUser().getPermissionNames(getSchemaContainer()));

			return Single.just(microschema);
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	@Override
	public MicroschemaReference transformToReference() {
		MicroschemaReference reference = createEmptyReferenceModel();
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
}
