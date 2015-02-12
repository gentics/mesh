package com.gentics.cailun.core.repository;

import java.util.List;

import com.gentics.cailun.core.rest.model.Content;

public interface CustomContentRepository {
	List<Content> findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);
}
