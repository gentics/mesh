package com.gentics.mesh.search.index.project;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class ProjectSearchHandler extends AbstractSearchHandler<Project, ProjectResponse> {

	@Inject
	public ProjectSearchHandler(LegacyDatabase db, SearchProvider searchProvider, ProjectIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
