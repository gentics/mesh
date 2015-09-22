package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer>implements MicroschemaContainerRoot {

	@Override
	protected Class<? extends MicroschemaContainer> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SCHEMA_CONTAINER;
	}

	@Override
	public void addMicroschema(MicroschemaContainer container) {
		addItem(container);
	}

	@Override
	public void removeMicroschema(MicroschemaContainer container) {
		removeItem(container);
	}

	@Override
	public MicroschemaContainer create(String name, User user) {
		throw new NotImplementedException();
	}

	@Override
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public void create(InternalActionContext rc, Handler<AsyncResult<MicroschemaContainer>> handler) {
		throw new NotImplementedException();
	}

}
