package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}
	 @Override
	public void create(RoutingContext rc, Handler<AsyncResult<MicroschemaContainer>> handler) {
		
	}

}
