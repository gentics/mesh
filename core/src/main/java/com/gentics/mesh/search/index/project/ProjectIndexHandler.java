package com.gentics.mesh.search.index.project;

import java.util.Collections;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.search.index.AbstractIndexHandler;

@Component
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	private static ProjectIndexHandler instance;

	private final static Set<String> indices = Collections.singleton(Project.TYPE);

	@Autowired
	private ProjectTransformator transformator;

	@PostConstruct
	public void setup() {
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
