package com.gentics.cailun.core.repository.project.generic;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.project.custom.ProjectUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class ProjectGenericNodeRepositoryImpl<T extends GenericNode> implements ProjectUUIDCRUDActions<T> {

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Override
	public T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria) {
		System.out.println("ougguöiggöi");
		return null;
	}

}
