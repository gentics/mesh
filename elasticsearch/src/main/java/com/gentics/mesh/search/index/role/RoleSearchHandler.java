package com.gentics.mesh.search.index.role;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.action.RoleDAOActions;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Handler for role index search operations.
 */
@Singleton
public class RoleSearchHandler extends AbstractSearchHandler<HibRole, RoleResponse> {

	@Inject
	public RoleSearchHandler(Database db, SearchProvider searchProvider, RoleIndexHandlerImpl indexHandler, MeshOptions options, RoleDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
