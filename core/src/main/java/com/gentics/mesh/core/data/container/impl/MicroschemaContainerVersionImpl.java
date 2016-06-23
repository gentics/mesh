package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

public class MicroschemaContainerVersionImpl
		extends AbstractGraphFieldSchemaContainerVersion<Microschema, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer>
		implements MicroschemaContainerVersion {

	public static void checkIndices(Database database) {
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

	@Override
	public List<? extends Micronode> getMicronodes() {
		return in(HAS_MICROSCHEMA_CONTAINER).has(MicronodeImpl.class).toListExplicit(MicronodeImpl.class);
	}

	@Override
	public Microschema getSchema() {
		Microschema microschema = ServerSchemaStorage.getInstance().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			try {
				microschema = JsonUtil.readValue(getJson(), MicroschemaModel.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			ServerSchemaStorage.getInstance().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	public void setSchema(Microschema microschema) {
		ServerSchemaStorage.getInstance().removeMicroschema(microschema.getName(), microschema.getVersion());
		ServerSchemaStorage.getInstance().addMicroschema(microschema);
		String json = JsonUtil.toJson(microschema);
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, microschema.getVersion());
	}

	@Override
	public Observable<Microschema> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		try {
			// Load the microschema and add/overwrite some properties 
			Microschema microschema = JsonUtil.readValue(getJson(), MicroschemaModel.class);
			microschema.setUuid(getSchemaContainer().getUuid());

			// Role permissions
			RestModelHelper.setRolePermissions(ac, getSchemaContainer(), microschema);
			microschema.setPermissions(ac.getUser().getPermissionNames(ac, getSchemaContainer()));

			return Observable.just(microschema);
		} catch (IOException e) {
			return Observable.error(e);
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

}
