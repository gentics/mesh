package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.Project;

public interface GlobalProjectRepository extends GlobalCaiLunNodeRepository<Project> {

	Project findByName(String string);

}
