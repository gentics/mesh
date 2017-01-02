package com.gentics.mesh.search.index.project;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

/**
 * Handler for the project specific search index.
 */
@Singleton
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	@Inject
	ProjectTransformator transformator;

	@Inject
	public ProjectIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	public ProjectTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Project.TYPE;
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return Project.TYPE;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Project.TYPE);
	}

	@Override
	public String getKey() {
		return Project.TYPE;
	}

	@Override
	protected RootVertex<Project> getRootVertex() {
		return boot.meshRoot().getProjectRoot();
	}

}
