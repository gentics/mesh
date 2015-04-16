package com.gentics.cailun.core.data.service;

import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ObjectSchemaRepository;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.PagingInfo;

@Component
public class ObjectSchemaServiceImpl extends GenericNodeServiceImpl<ObjectSchema> implements ObjectSchemaService {

	@Autowired
	ObjectSchemaRepository schemaRepository;

	@Autowired
	Neo4jTemplate neo4jTemplate;

	@Override
	public Result<ObjectSchema> findAll() {
		return schemaRepository.findAll();
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
	public ObjectSchemaResponse transformToRest(ObjectSchema schema) {
		if (schema == null) {
			throw new HttpStatusCodeErrorException(500, "Schema can't be null");
		}
		ObjectSchemaResponse schemaForRest = new ObjectSchemaResponse();
		schemaForRest.setDescription(schema.getDescription());
		schemaForRest.setDisplayName(schema.getDisplayName());
		schemaForRest.setName(schema.getName());
		schemaForRest.setUuid(schema.getUuid());
		// TODO creator

		// TODO we need to add checks that prevents multiple schemas with the same key
		for (PropertyTypeSchema propertyTypeSchema : schema.getPropertyTypeSchemas()) {
			propertyTypeSchema = neo4jTemplate.fetch(propertyTypeSchema);
			PropertyTypeSchemaResponse propertyTypeSchemaForRest = new PropertyTypeSchemaResponse();
			propertyTypeSchemaForRest.setUuid(propertyTypeSchema.getUuid());
			propertyTypeSchemaForRest.setKey(propertyTypeSchema.getKey());
			propertyTypeSchemaForRest.setDescription(propertyTypeSchema.getDescription());
			propertyTypeSchemaForRest.setType(propertyTypeSchema.getType().getName());
			propertyTypeSchemaForRest.setDisplayName(propertyTypeSchema.getDisplayName());
			schemaForRest.getPropertyTypeSchemas().add(propertyTypeSchemaForRest);
		}
		Collections.sort(schemaForRest.getPropertyTypeSchemas(), new PropertTypeSchemaComparator());
		// Sort the property types schema. Otherwise rest response is erratic

		for (Project project : schema.getProjects()) {
			project = neo4jTemplate.fetch(project);
			ProjectResponse restProject = new ProjectResponse();
			restProject.setUuid(project.getUuid());
			restProject.setName(project.getName());
			schemaForRest.getProjects().add(restProject);
		}
		// Sort the list by project name
		Collections.sort(schemaForRest.getProjects(), new Comparator<ProjectResponse>() {
			@Override
			public int compare(ProjectResponse o1, ProjectResponse o2) {
				return o1.getName().compareTo(o2.getName());
			};
		});
		return schemaForRest;
	}

	@Override
	public void deleteByUUID(String uuid) {
		schemaRepository.deleteByUuid(uuid);
	}

	@Override
	public Page<ObjectSchema> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		return schemaRepository.findAll(requestUser, new PageRequest(pagingInfo.getPage(), pagingInfo.getPerPage()));
	}

	@Override
	public ObjectSchema findByName(String name) {
		return schemaRepository.findByName(name);
	}
}

class PropertTypeSchemaComparator implements Comparator<PropertyTypeSchemaResponse> {
	@Override
	public int compare(PropertyTypeSchemaResponse o1, PropertyTypeSchemaResponse o2) {
		return o1.getKey().compareTo(o2.getKey());
	}
}