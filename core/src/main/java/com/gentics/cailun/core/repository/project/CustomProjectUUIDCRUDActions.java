package com.gentics.cailun.core.repository.project;

import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.rest.model.CaiLunNode;

@NoRepositoryBean
public interface CustomProjectUUIDCRUDActions<T extends CaiLunNode> {
	
	T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);

}
