package com.gentics.cailun.core.repository;

import java.util.List;

import com.gentics.cailun.core.rest.model.LocalizedContent;

public interface CustomContentRepository {
	List<LocalizedContent> findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);
}
