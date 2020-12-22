package com.gentics.mesh.search.index.user;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Handler for elasticsearch user index related queries.
 */
@Singleton
public class UserSearchHandler extends AbstractSearchHandler<HibUser, UserResponse> {

	@Inject
	public UserSearchHandler(Database db, SearchProvider searchProvider, AbstractMeshOptions options, UserIndexHandlerImpl indexHandler, UserDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
