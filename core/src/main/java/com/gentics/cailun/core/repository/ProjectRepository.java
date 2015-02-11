package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.Project;

public interface ProjectRepository extends GraphRepository<Project> {

}
