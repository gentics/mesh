package com.gentics.cailun.core.data.service.generic;

import java.util.List;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.generic.GenericNode;

public interface GenericNodeService<T extends GenericNode> {

	public T save(T node);

	public void save(List<T> nodes);

	public void delete(T node);
	
	public void deleteByUUID(String uuid);

	public T findOne(Long id);

	public Result<T> findAll();

	public Iterable<T> findAll(String projectName);

	public T findByName(String projectName, String name);

	public T findByUUID(String projectName, String uuid);

	public T findByUUID(String uuid);

	public T reload(T node);

}
