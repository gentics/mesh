package com.gentics.mesh.core.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.ProjectRoot;
import com.gentics.mesh.core.repository.action.ProjectActions;
import com.gentics.mesh.core.repository.generic.GenericNodeRepositoryImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;

public class ProjectRepositoryImpl extends GenericNodeRepositoryImpl<Project> implements ProjectActions {

	private static final Logger log = LoggerFactory.getLogger(ProjectRepositoryImpl.class);

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private MeshSpringConfiguration springConfig;

	@Override
	public Project save(Project project) {
		ProjectRoot root = projectRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The project root node could not be found.");
		}
		project = neo4jTemplate.save(project);
		root.getProjects().add(project);
		neo4jTemplate.save(root);
		return project;
	}

}
