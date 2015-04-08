package com.gentics.cailun.core.repository.action;

import java.util.List;

import com.gentics.cailun.core.data.model.Content;

public interface GenericContentRepositoryActions<T extends Content> {
	List<T> findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);
}
