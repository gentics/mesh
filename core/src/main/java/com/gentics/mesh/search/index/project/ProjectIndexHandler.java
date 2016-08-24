package com.gentics.mesh.search.index.project;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	private static ProjectIndexHandler instance;

	private final static Set<String> indices = Collections.singleton(Project.TYPE);

	private ProjectTransformator transformator = new ProjectTransformator();

	private BootstrapInitializer boot;

	public ProjectIndexHandler(BootstrapInitializer boot, SearchProvider searchProvider, Database db, IndexHandlerRegistry registry) {
		super(searchProvider, db, registry);
		this.boot = boot;
		instance = this;
	}

	public ProjectTransformator getTransformator() {
		return transformator;
	}

	public static ProjectIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Project.TYPE;
	}

	@Override
	public Set<String> getIndices() {
		return indices;
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	protected String getType() {
		return Project.TYPE;
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
