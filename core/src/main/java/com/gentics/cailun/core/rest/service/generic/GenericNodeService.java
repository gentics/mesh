package com.gentics.cailun.core.rest.service.generic;

import java.util.List;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface GenericNodeService<T extends GenericNode> {

	public void setProperty(T node, Language language, String key, String value);

	public void setName(T node, Language language, String name);

	public T save(T node);

	public void save(List<T> nodes);

	public void delete(T node);

	public T findOne(Long id);

	public Result<T> findAll();

}
