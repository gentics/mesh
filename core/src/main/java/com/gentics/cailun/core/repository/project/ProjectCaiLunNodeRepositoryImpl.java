package com.gentics.cailun.core.repository.project;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.project.custom.ProjectUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class ProjectCaiLunNodeRepositoryImpl<T extends CaiLunNode> implements ProjectUUIDCRUDActions<T> {

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Override
	public T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria) {
		System.out.println("ougguöiggöi");
		return null;
	}

}
