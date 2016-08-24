package com.gentics.mesh.search.index.user;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class UserIndexHandler extends AbstractIndexHandler<User> {

	private final static Set<String> indices = Collections.singleton(User.TYPE);

	private static UserIndexHandler instance;

	private UserTransformator transformator;

	private BootstrapInitializer boot;

	@Inject
	public UserIndexHandler(UserTransformator transformator, BootstrapInitializer boot, SearchProvider searchProvider, Database db,
			IndexHandlerRegistry registry) {
		super(searchProvider, db, registry);
		this.transformator = transformator;
		this.boot = boot;
		instance = this;
	}

	public static UserIndexHandler getInstance() {
		return instance;
	}

	public UserTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return User.TYPE;
	}

	@Override
	public Set<String> getIndices() {
		return indices;
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	protected String getType() {
		return User.TYPE;
	}

	@Override
	public String getKey() {
		return User.TYPE;
	}

	@Override
	protected RootVertex<User> getRootVertex() {
		return boot.meshRoot().getUserRoot();
	}
}
