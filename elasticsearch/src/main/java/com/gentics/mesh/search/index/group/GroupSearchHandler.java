package com.gentics.mesh.search.index.group;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.actions.GroupDAOActions;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class GroupSearchHandler extends AbstractSearchHandler<Group, GroupResponse> {

	@Inject
	public GroupSearchHandler(Database db, SearchProvider searchProvider, GroupIndexHandler indexHandler, MeshOptions options,
		GroupDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
