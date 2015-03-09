package com.gentics.cailun.core.data.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ObjectSchemaRepository;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.core.rest.schema.response.PropertyTypeSchemaResponse;

@Component
@Transactional
public class ObjectSchemaServiceImpl extends GenericNodeServiceImpl<ObjectSchema> implements ObjectSchemaService {

	@Autowired
	ObjectSchemaRepository schemaRepository;

	@Override
	public List<ObjectSchema> findAll(String projectName) {
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			List<ObjectSchema> list = new ArrayList<>();
			for (ObjectSchema schema : schemaRepository.findAll(projectName)) {
				list.add(schema);
			}
			tx.success();
			return list;
		}
	}

	@Override
	public ObjectSchema findByUUID(String projectName, String uuid) {
		return schemaRepository.findByUUID(projectName, uuid);
	}

	@Override
	public ObjectSchema findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return schemaRepository.findByName(projectName, name);
	}

	@Override
	public ObjectSchemaResponse getReponseObject(ObjectSchema schema) {
		ObjectSchemaResponse schemaForRest = new ObjectSchemaResponse();
		schemaForRest.setDescription(schema.getDescription());
		schemaForRest.setName(schema.getName());
		schemaForRest.setUuid(schema.getUuid());
		// TODO creator

		// Sort the property types schema. Otherwise rest response is erratic
		Set<PropertyTypeSchema> treeSet = new TreeSet<PropertyTypeSchema>(new PropertTypeSchemaComparator());
		// TODO we need to add checks that prevents multiple schemas with the same key
		treeSet.addAll(schema.getPropertyTypeSchemas());

		for (PropertyTypeSchema propertyTypeSchema : treeSet) {
			PropertyTypeSchemaResponse propertyTypeSchemaForRest = new PropertyTypeSchemaResponse();
			propertyTypeSchemaForRest.setUuid(propertyTypeSchema.getUuid());
			propertyTypeSchemaForRest.setKey(propertyTypeSchema.getKey());
			propertyTypeSchemaForRest.setDescription(propertyTypeSchema.getDescription());
			propertyTypeSchemaForRest.setType(propertyTypeSchema.getType().getName());
			schemaForRest.getPropertyTypeSchemas().add(propertyTypeSchemaForRest);
		}
		return schemaForRest;
	}

	@Override
	public void deleteByName(String projectName, String schemaName) {
		schemaRepository.deleteByName(projectName, schemaName);
	}

	@Override
	public void deleteByUUID(String uuid) {
		schemaRepository.deleteByUuid(uuid);
	}
}

class PropertTypeSchemaComparator implements Comparator<PropertyTypeSchema> {
	@Override
	public int compare(PropertyTypeSchema o1, PropertyTypeSchema o2) {
		return o1.getKey().compareTo(o2.getKey());
	}
}