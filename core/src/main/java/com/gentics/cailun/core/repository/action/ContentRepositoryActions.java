package com.gentics.cailun.core.repository.action;

import java.util.List;

import com.gentics.cailun.core.rest.model.Content;

public interface ContentRepositoryActions<T extends Content> {
	List<T> findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);
}
