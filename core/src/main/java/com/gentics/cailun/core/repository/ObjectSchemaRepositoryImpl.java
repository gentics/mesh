package com.gentics.cailun.core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.ObjectSchemaRoot;
import com.gentics.cailun.core.repository.action.ObjectSchemaActions;

public class ObjectSchemaRepositoryImpl implements ObjectSchemaActions {

	@Autowired
	private ObjectSchemaRepository schemaRepository;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Override
	public ObjectSchema save(ObjectSchema schema) {
		ObjectSchemaRoot root = schemaRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The schema root node could not be found.");
		}
		schema = neo4jTemplate.save(schema);
		root.getSchemas().add(schema);
		neo4jTemplate.save(root);
		return schema;
	}

}
