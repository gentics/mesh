package com.gentics.mesh.core.data.service.generic;

import org.jglue.totorom.FramedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.generic.GenericNode;

@Component
public class GenericNodeServiceImpl<T extends GenericNode> implements GenericNodeService<T> {

	@Autowired
	protected FramedGraph framedGraph;

	@Override
	public void delete(T node) {
	}

	@Override
	public T findOne(Long id) {
		//		return nodeRepository.findOne(id);
		return null;
	}

	@Override
	public T findByName(String project, String name) {
		//		return nodeRepository.findByI18Name(project, name);
		return null;
	}

	@Override
	public T findByUUID(String project, String uuid) {
		//		return nodeRepository.findByUUID(project, uuid);
		return null;
	}

	@Override
	public T findByUUID(String uuid) {
		//		return nodeRepository.findByUUID(uuid);
		return null;
	}

	@Override
	public void deleteByUUID(String uuid) {
		//		nodeRepository.deleteByUuid(uuid);
	}

	//	@Query("MATCH (n:GenericNode)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<T> findAll(String projectName) {
		return null;
	}

}
