package com.gentics.cailun.core.data.service.generic;

import java.util.List;
import java.util.UUID;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.repository.I18NValueRepository;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.neo4j.UUIDTransactionEventHandler;

@Component
public class GenericNodeServiceImpl<T extends GenericNode> implements GenericNodeService<T> {

	@Autowired
	protected I18NValueRepository i18nPropertyRepository;

	@Autowired
	@Qualifier("genericNodeRepository")
	protected GenericNodeRepository<T> nodeRepository;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Autowired
	protected GraphDatabaseService database;

	@Autowired
	protected Neo4jTemplate neo4jTemplate;

	@Override
	public T save(T node) {
		if (node.isNew() && node.getUuid() == null) {
			node.setUuid(UUIDTransactionEventHandler.getUUID());
		}
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
	public Iterable<T> findAll(String project) {
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

	@Override
	public void deleteByUUID(String uuid) {
		nodeRepository.deleteByUuid(uuid);
	}

	@Override
	public T projectTo(Node node, Class<T> clazz) {
		try (Transaction t = database.beginTx()) {
			T entity = neo4jTemplate.projectTo(node, clazz);
			t.success();
			return entity;
		}
	}
}
