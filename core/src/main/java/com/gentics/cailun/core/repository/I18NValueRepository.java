package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.I18NProperties;

public interface I18NValueRepository extends GraphRepository<I18NProperties> {

}
