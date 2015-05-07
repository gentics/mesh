package com.gentics.mesh.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.mesh.core.data.model.I18NProperties;

public interface I18NValueRepository extends GraphRepository<I18NProperties> {

}
