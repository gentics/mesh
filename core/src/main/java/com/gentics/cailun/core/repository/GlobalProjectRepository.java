package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.generic.GlobalGenericNodeRepository;
import com.gentics.cailun.core.rest.model.Project;

public interface GlobalProjectRepository extends GlobalGenericNodeRepository<Project> {

	Project findByName(String string);

}
