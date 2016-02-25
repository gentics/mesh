package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

public class MicroschemaContainerImpl extends AbstractGraphFieldSchemaContainer<Microschema, MicroschemaContainer, MicroschemaReference>
		implements MicroschemaContainer {

	private static final String NAME_PROPERTY_KEY = "name";

	@Override
	protected Class<MicroschemaContainerImpl> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	public static void checkIndices(Database database) {
		database.addVertexType(MicroschemaContainerImpl.class);
	}

	@Override
	protected String getMigrationAddress() {
		return NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS;
	}

	@Override
	public MicroschemaReference createEmptyReferenceModel() {
		return new MicroschemaReference();
	}

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public String getName() {
		return getProperty(NAME_PROPERTY_KEY);
	}

	@Override
	public void setName(String name) {
		setProperty(NAME_PROPERTY_KEY, name);
	}

	@Override
	public Microschema getSchema() {
		Microschema microschema = ServerSchemaStorage.getInstance().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			try {
				microschema = JsonUtil.readSchema(getJson(), MicroschemaModel.class);
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
	public Observable<Microschema> transformToRestSync(InternalActionContext ac, String... languageTags) {
		try {
			// Load the microschema and add/overwrite some properties 
			Microschema microschema = JsonUtil.readSchema(getJson(), MicroschemaModel.class);
			microschema.setUuid(getUuid());

			// Role permissions
			RestModelHelper.setRolePermissions(ac, this, microschema);

			microschema.setPermissions(ac.getUser().getPermissionNames(ac, this));

			return Observable.just(microschema);
		} catch (IOException e) {
			return Observable.error(e);
		}
	}

	@Override
	public void delete() {
		addIndexBatch(DELETE_ACTION);
		getElement().remove();
	}

	@Override
	@Deprecated
	public Observable<? extends MicroschemaContainer> update(InternalActionContext ac) {
		throw new NotImplementedException("Updating is not directly supported for microschemas. Please start a microschema migration.");
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<? extends Micronode> getMicronodes() {
		return in(HAS_MICROSCHEMA_CONTAINER).has(MicronodeImpl.class).toListExplicit(MicronodeImpl.class);
	}

	private String getJson() {
		return getProperty("json");
	}

	private void setJson(String json) {
		setProperty("json", json);
	}

}
