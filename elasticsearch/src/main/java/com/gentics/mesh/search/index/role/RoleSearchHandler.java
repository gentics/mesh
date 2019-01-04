package com.gentics.mesh.search.index.role;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class RoleSearchHandler extends AbstractSearchHandler<Role, RoleResponse> {

	@Inject
	public RoleSearchHandler(LegacyDatabase db, SearchProvider searchProvider, RoleIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
