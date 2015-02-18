package com.gentics.cailun.core.repository.project.custom;

import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.rest.model.generic.GenericNode;

@NoRepositoryBean
public interface ProjectUUIDCRUDActions<T extends GenericNode> {
	
	T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);

}
