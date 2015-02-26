package com.gentics.cailun.core.data.service.generic;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.repository.I18NValueRepository;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

@Component
@Transactional
public class GenericNodeServiceImpl<T extends GenericNode> implements GenericNodeService<T> {

	@Autowired
	I18NValueRepository i18nPropertyRepository;

	@Autowired
	@Qualifier("genericNodeRepository")
	GenericNodeRepository<T> nodeRepository;

	@Override
	public T save(T node) {
		// TODO invoke a reload afterwards - otherwise the uuid is null and succeeding saving will fail.
		return nodeRepository.save(node);
	}

	@Override
	public void delete(T node) {
		nodeRepository.delete(node);
	}

	@Override
	public T findOne(Long id) {
		return nodeRepository.findOne(id);
	}

	@Override
	public void save(List<T> nodes) {
		this.nodeRepository.save(nodes);
	}

	@Override
	public Result<T> findAll() {
		return nodeRepository.findAll();

	}

	@Override
	public Result<T> findAll(String project) {
		return nodeRepository.findAll(project);
	}

	@Override
	public T findByName(String project, String name) {
		return nodeRepository.findByI18Name(project, name);
	}

	@Override
	public T findByUUID(String project, String uuid) {
		return nodeRepository.findByUUID(project, uuid);
	}

	@Override
	public T reload(T node) {
		return nodeRepository.findOne(node.getId());
	}

	@Override
	public T findByUUID(String uuid) {
		return nodeRepository.findByUUID(uuid);
	}

}
