package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.action.ProjectActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.core.rest.model.Project;

public interface ProjectRepository extends GenericNodeRepository<Project>, ProjectActions {

	Project findByName(String string);

}
