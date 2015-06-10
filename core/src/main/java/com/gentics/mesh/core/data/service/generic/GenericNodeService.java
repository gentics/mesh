package com.gentics.mesh.core.data.service.generic;

import com.gentics.mesh.core.data.model.generic.GenericNode;

public interface GenericNodeService<T extends GenericNode> {

	public void delete(T node);

	public void deleteByUUID(String uuid);

	public T findOne(Long id);

	public T findByName(String projectName, String name);

	public T findByUUID(String projectName, String uuid);

	public T findByUUID(String uuid);

}
