package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.Project;

public interface ProjectRepository extends CaiLunNodeRepository<Project> {

	Project findByName(String string);

}
