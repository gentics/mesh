package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.CaiLunNode;

public interface CaiLunNodeRepository<T extends CaiLunNode> extends GraphRepository<T> {

}
