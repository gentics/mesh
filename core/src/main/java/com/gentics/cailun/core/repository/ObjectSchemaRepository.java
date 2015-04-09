package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.ObjectSchemaRoot;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.action.ObjectSchemaActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface ObjectSchemaRepository extends UUIDCRUDActions<ObjectSchema>, ObjectSchemaActions {

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

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
	public Page<ObjectSchema> findAll(User requestUser, Pageable pageable);

	@Query("MATCH (n:ObjectSchemaRoot) return n")
	ObjectSchemaRoot findRoot();

	Result<ObjectSchema> findAll();

}
