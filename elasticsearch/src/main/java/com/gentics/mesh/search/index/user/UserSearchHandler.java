package com.gentics.mesh.search.index.user;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class UserSearchHandler extends AbstractSearchHandler<User, UserResponse> {

	@Inject
	public UserSearchHandler(LegacyDatabase db, SearchProvider searchProvider, UserIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
