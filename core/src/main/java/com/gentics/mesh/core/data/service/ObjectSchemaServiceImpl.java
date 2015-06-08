package com.gentics.mesh.core.data.service;

import java.awt.print.Pageable;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.root.ObjectSchemaRoot;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyTypeSchema;
import com.gentics.mesh.core.data.model.schema.propertytypes.MicroPropertyTypeSchema;
import com.gentics.mesh.core.data.model.schema.propertytypes.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class ObjectSchemaServiceImpl extends GenericNodeServiceImpl<ObjectSchema> implements ObjectSchemaService {

	@Override
	public Result<ObjectSchema> findAll() {
		return null;
	}

	@Override
	public ObjectSchema findByUUID(String projectName, String uuid) {
		return null;
	}

	@Override
	public ObjectSchema findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		// @Query("MATCH (project:Project)-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema) WHERE n.name = {1} AND project.name = {0} RETURN n")
		// TODO fix query - somehow the project relationship is not matching
		//			@Query("MATCH (n:ObjectSchema) WHERE n.name = {1} RETURN n")
		return null;
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
		for (BasicPropertyTypeSchema propertyTypeSchema : schema.getPropertyTypeSchemas()) {
//			propertyTypeSchema = neo4jTemplate.fetch(propertyTypeSchema);
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
//			project = neo4jTemplate.fetch(project);
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
	}

	@Override
	public Page<ObjectSchema> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		//		return findAll(requestUser, new MeshPageRequest(pagingInfo));
		return null;
	}

	public BasicPropertyTypeSchema getPropertyTypeSchema(String typeKey) {
//		if (StringUtils.isEmpty(typeKey)) {
//			return null;
//		}
//		for (BasicPropertyTypeSchema propertyTypeSchema : propertyTypeSchemas) {
//			if (propertyTypeSchema.getKey().equals(typeKey)) {
//				return propertyTypeSchema;
//			}
//		}
//		return null;
		return null;
	}

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	public void deleteByName(String projectName, String schemaName) {
		//		@Query("MATCH (project:Project)<-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema {name: {1}}) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema)-[rp]-() OPTIONAL MATCH (n)-[r2]-() WHERE project.name = {0} DELETE n,r,p,r2,rp")
	}

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	public void deleteByUuid(String uuid) {
		//		@Query("MATCH (n:ObjectSchema {uuid: {0}}) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema)-[rp]-() OPTIONAL MATCH (n)-[r2]-() DELETE n,r,p,r2,rp")
	}

	public Iterable<ObjectSchema> findAll(String projectName) {
		//		@Query("MATCH (n:ObjectSchema)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
		return null;
	}

	public Page<ObjectSchema> findAll(User requestUser, Pageable pageable) {
		//		@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
		return null;
	}

	public ObjectSchemaRoot findRoot() {
		//	@Query("MATCH (n:ObjectSchemaRoot) return n")
		return null;
	}

	@Override
	public ObjectSchema save(ObjectSchema schema) {
		//		ObjectSchemaRoot root = schemaService.findRoot();
		//		if (root == null) {
		//			throw new NullPointerException("The schema root node could not be found.");
		//		}
		//		schema = neo4jTemplate.save(schema);
		//		root.getSchemas().add(schema);
		//		neo4jTemplate.save(root);
		//		return schema;
		return null;
	}

	@Override
	public ObjectSchema findByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectSchema create(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectSchemaRoot createRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BasicPropertyTypeSchema create(String nameKeyword, PropertyType i18nString) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MicroPropertyTypeSchema createMicroPropertyTypeSchema(String key) {

//		public MicroPropertyTypeSchema(String name) {
//			//		super(name, PropertyType.MICROSCHEMA);
//			//	}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BasicPropertyTypeSchema createBasicPropertyTypeSchema(String key, PropertyType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BasicPropertyTypeSchema createListPropertyTypeSchema(String key) {
		// TODO Auto-generated method stub
		return null;
	}

}

class PropertTypeSchemaComparator implements Comparator<PropertyTypeSchemaResponse> {
	@Override
	public int compare(PropertyTypeSchemaResponse o1, PropertyTypeSchemaResponse o2) {
		return o1.getKey().compareTo(o2.getKey());
	}
}