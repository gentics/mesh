package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.repository.action.ProjectActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface ProjectRepository extends GenericNodeRepository<Project>, ProjectActions {

	Project findByName(String string);

}
