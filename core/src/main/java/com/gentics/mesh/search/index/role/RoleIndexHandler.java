package com.gentics.mesh.search.index.role;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

/**
 * Handler for the elasticsearch role index.
 */
@Singleton
public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	@Inject
	RoleTransformator transformator;

	@Inject
	public RoleIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		super(searchProvider, db, boot);
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Role.TYPE;
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return Role.TYPE;
	}

	@Override
	public RoleTransformator getTransformator() {
		return transformator;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Role.TYPE);
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
