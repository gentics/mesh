package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.core.rest.model.Project;

public interface ProjectRepository extends GenericNodeRepository<Project> {

	Project findByName(String string);

}
