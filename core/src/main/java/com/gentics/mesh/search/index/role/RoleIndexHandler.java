package com.gentics.mesh.search.index.role;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	private static RoleIndexHandler instance;

	private RoleTransformator transformator = new RoleTransformator();

	private final static Set<String> indices = Collections.singleton(Role.TYPE);

	public RoleIndexHandler(BootstrapInitializer boot, SearchProvider searchProvider, Database db, IndexHandlerRegistry registry) {
		super(boot, searchProvider, db, registry);
		instance = this;
	}

	public static RoleIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Role.TYPE;
	}

	@Override
	public RoleTransformator getTransformator() {
		return transformator;
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
		return Role.TYPE;
	}

	@Override
	public String getKey() {
		return Role.TYPE;
	}

	@Override
	protected RootVertex<Role> getRootVertex() {
		return boot.meshRoot().getRoleRoot();
	}

}
