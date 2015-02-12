package com.gentics.cailun.core.repository.project.custom;

import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.rest.model.CaiLunNode;

@NoRepositoryBean
public interface ProjectUUIDCRUDActions<T extends CaiLunNode> {
	
	T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);

}
