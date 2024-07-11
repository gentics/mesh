package com.gentics.mesh.search.index.group;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;
import com.gentics.mesh.util.SearchWaitUtil;

/**
 * Handler for group search related index operations.
 */
@Singleton
public class GroupSearchHandler extends AbstractSearchHandler<Group, GroupResponse> {

	@Inject
	public GroupSearchHandler(Database db, SearchProvider searchProvider, GroupIndexHandler indexHandler, MeshOptions options,
		GroupDAOActions actions, SearchWaitUtil waitUtil) {
		super(db, searchProvider, options, indexHandler, actions, waitUtil);
	}

}
