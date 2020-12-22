package com.gentics.mesh.search.index.project;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Handler for ES project search.
 */
@Singleton
public class ProjectSearchHandler extends AbstractSearchHandler<HibProject, ProjectResponse> {

	@Inject
	public ProjectSearchHandler(Database db, SearchProvider searchProvider, ProjectIndexHandlerImpl indexHandler, MeshOptions options,
		ProjectDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
