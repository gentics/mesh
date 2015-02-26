package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface ObjectSchemaRepository extends GenericNodeRepository<ObjectSchema> {

	// @Query("MATCH (project:Project)-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema) WHERE n.name = {1} AND project.name = {0} RETURN n")
	// TODO fix query - somehow the project relationship is not matching
	@Query("MATCH (n:ObjectSchema) WHERE n.name = {1} RETURN n")
	ObjectSchema findByName(String projectName, String name);

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	@Query("MATCH (project:Project)<-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema {name: {1}}), OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema), OPTIONAL MATCH (n)-[r2]-() WHERE project.name = {0} DELETE n,r,p,r2")
	public void deleteByName(String projectName, String schemaName);

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	@Query("MATCH (n:ObjectSchema {uuid: {0}}), OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema), OPTIONAL MATCH (n)-[r2]-() DELETE n,r,p,r2")
	public void deleteByUuid(String uuid);
	
	@Query("MATCH (n:ObjectSchema)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	public Iterable<ObjectSchema> findAll(String projectName);

}
