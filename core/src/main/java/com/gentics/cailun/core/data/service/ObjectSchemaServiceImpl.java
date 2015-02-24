package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ObjectSchemaRepository;

@Component
@Transactional
public class ObjectSchemaServiceImpl extends GenericNodeServiceImpl<ObjectSchema> implements ObjectSchemaService {

	@Autowired
	ObjectSchemaRepository schemaRepository;

	@Override
	public Result<ObjectSchema> findAll() {
		return schemaRepository.findAll();
	}

	@Override
	public Result<ObjectSchema> findAll(String project) {
		// TODO Impl
		return null;
	}

	@Override
	public ObjectSchema findByUUID(String project, String uuid) {
		schemaRepository.findByUUID(project, uuid);
		return null;
	}

	@Override
	public ObjectSchema findByName(String project, String name) {
		schemaRepository.findByName(project, name);
		return null;
	}
}
