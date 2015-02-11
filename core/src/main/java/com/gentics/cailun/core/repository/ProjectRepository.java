package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.Project;

public interface ProjectRepository extends UUIDGraphRepository<Project> {

	Project findByName(String string);

}
